package com.financebuddha.finbud.hrms.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final DeviceApiKeyFilter deviceApiKeyFilter;

    /**
     * C-4: comma-separated list of allowed CORS origins, sourced from
     * {@code app.cors.allowed-origins}. The Vite dev server defaults to
     * {@code http://localhost:5173}; production should override via env
     * (e.g. {@code APP_CORS_ALLOWED_ORIGINS=https://hrms.finbud.in}).
     * <p>
     * We deliberately do NOT use {@code "*"} — it's incompatible with
     * {@code allowCredentials=true} and forbidden by modern browsers when
     * the request is credentialed.
     */
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String corsAllowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ---------------- Public endpoints ----------------
                .requestMatchers("/api/auth/login", "/api/auth/register",
                                 "/api/auth/refresh-token").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/health").permitAll()

                // ---------------- Portal punch flow ----------------
                // T2-2: attendance is now portal-first — every employee marks
                // themselves in/out from the browser with a valid JWT. The
                // controller uses @CurrentUser UserPrincipal to resolve the
                // caller, so the JWT filter MUST populate the security context
                // before the request reaches the controller. DeviceApiKeyFilter
                // is retained for backwards compatibility; when no device key
                // is configured (the default) it short-circuits to a no-op.
                .requestMatchers(HttpMethod.POST,
                        "/api/attendance/punch-in", "/api/attendance/punch-out").authenticated()

                // ---------------- Admin user management (ADMIN + HR) ----------------
                // HR needs to read/edit user accounts to run the role editor +
                // reset-password workflows, but HR cannot grant privileged
                // roles — that check is enforced inside AdminUserServiceImpl.
                // The POST (create-user) endpoint keeps its ADMIN-only
                // @PreAuthorize annotation. This matcher MUST precede the
                // blanket /api/admin/** matcher below (first-match-wins).
                .requestMatchers("/api/admin/users/**").hasAnyRole("ADMIN", "HR")

                // ---------------- Admin only ----------------
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // ---------------- Avatar (self or Admin/HR) ----------------
                // These matchers MUST precede the blanket /api/employees/**
                // write rules below — rule order is first-match-wins, so if the
                // HR/Admin rule matched first a regular employee would be
                // blocked from uploading their own picture before the
                // controller's method-level @PreAuthorize ever ran. Auth is
                // still required; EmployeeAvatarController enforces
                //   hasAnyRole('ADMIN','HR') or principal.employeeId == #code
                .requestMatchers(HttpMethod.POST,   "/api/employees/*/avatar").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/employees/*/avatar").authenticated()

                // ---------------- HR + Admin ----------------
                .requestMatchers(HttpMethod.POST,   "/api/employees/**").hasAnyRole("ADMIN", "HR")
                .requestMatchers(HttpMethod.PUT,    "/api/employees/**").hasAnyRole("ADMIN", "HR")
                .requestMatchers(HttpMethod.DELETE, "/api/employees/**").hasAnyRole("ADMIN", "HR")
                .requestMatchers("/api/payroll/**").hasAnyRole("ADMIN", "HR")
                .requestMatchers("/api/shifts/**").hasAnyRole("ADMIN", "HR")
                .requestMatchers("/api/departments/**").hasAnyRole("ADMIN", "HR")

                // ---------------- Reports ----------------
                // Commitment exports are scoped to the caller's own reporting
                // subtree, so every supervisor level needs through this gate.
                // The URL rule is deliberately coarse — it cannot tell WHICH
                // team is being requested. The per-request ownership check is
                // AuthzService#requireCanExportTeamReport /
                // #requireCanExportEmployeeReport, called by
                // CommitmentReportController. Widening this line without those
                // guards in place would let any manager read any other
                // manager's team.
                .requestMatchers("/api/reports/commitment/**")
                    .hasAnyRole("ADMIN", "HR", "MANAGER", "TEAM_LEADER", "ATL")
                .requestMatchers("/api/reports/**").hasAnyRole("ADMIN", "HR", "MANAGER")

                // ---------------- ATL dashboard ----------------
                // Self-service ("my team's commitment") must precede the
                // blanket /api/atl/** rule below — same first-match-wins
                // reasoning as the avatar matchers above. Any authenticated
                // user can hit /me/team-commitment; it's inherently scoped
                // to their own employee id by the controller.
                .requestMatchers("/api/atl/me/**").authenticated()
                .requestMatchers("/api/atl/**").hasAnyRole("ADMIN", "HR", "ATL")

                // ---------------- Hierarchy target-flow dashboards ----------------
                // Every authenticated user reads their OWN tiered dashboard; the
                // controller scopes it to their employee id and role. The flat
                // "all employees" overview is ADMIN/HR only (also enforced by the
                // method-level @PreAuthorize as defense-in-depth).
                .requestMatchers("/api/hierarchy/dashboard/me").authenticated()
                .requestMatchers("/api/hierarchy/all-employees").hasAnyRole("ADMIN", "HR")
                .requestMatchers("/api/hierarchy/**")
                        .hasAnyRole("ADMIN", "HR", "MANAGER", "TEAM_LEADER", "ATL")

                // ---------------- Manager + above ----------------
                // C-5: the actual leave routes are /api/leaves/{id}/approve and
                // /api/leaves/{id}/reject — the old "/api/leaves/approve/**"
                // matcher never matched and was a silent gap. The wildcard
                // segment in /api/leaves/*/approve handles a single path
                // variable; controller methods carry @PreAuthorize as
                // defense-in-depth.
                .requestMatchers(HttpMethod.POST, "/api/leaves/*/approve").hasAnyRole("ADMIN", "HR", "MANAGER")
                .requestMatchers(HttpMethod.POST, "/api/leaves/*/reject").hasAnyRole("ADMIN", "HR", "MANAGER")

                // ---------------- Self-service ----------------
                .requestMatchers("/api/employees/me/**").authenticated()
                .requestMatchers("/api/attendance/me/**").authenticated()
                .requestMatchers("/api/leaves/me/**").authenticated()
                .requestMatchers("/api/payroll/me/**").authenticated()

                // ---------------- Default ----------------
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            // DeviceApiKeyFilter MUST come before the JWT filter — it short-circuits
            // device endpoints without consulting the JWT machinery, and only runs
            // for the protected punch routes (see DeviceApiKeyFilter#shouldNotFilter).
            // We anchor the device filter directly on JwtAuthenticationFilter.class
            // (not UsernamePasswordAuthenticationFilter) to make the ordering
            // explicit rather than relying on insertion-order tie-breaking.
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(deviceApiKeyFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = new java.util.ArrayList<>(Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList());

        // Always allow the production frontend (Vercel), regardless of the
        // APP_CORS_ALLOWED_ORIGINS env value. A missing or out-of-date env var
        // must not silently block the live site with a CORS failure.
        for (String required : List.of("https://hrms-frontend-main-three.vercel.app")) {
            if (!origins.contains(required)) {
                origins.add(required);
            }
        }

        if (origins.isEmpty()) {
            // Fail-safe so a misconfigured env doesn't accidentally allow everything.
            origins = List.of("http://localhost:5173");
        }

        // Use setAllowedOriginPatterns rather than setAllowedOrigins so we can
        // later swap in patterns like https://*.finbud.in without changing code.
        configuration.setAllowedOriginPatterns(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "X-Device-Api-Key"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
