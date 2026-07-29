package com.financebuddha.finbud.hrms.security;

import com.financebuddha.finbud.hrms.service.SystemConfigService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * C-3: API-key gate for the biometric punch device endpoints.
 * <p>
 * The fingerprint terminal posts to {@code /api/attendance/punch-in} and
 * {@code /api/attendance/punch-out} with no JWT (the device is not a user).
 * Before C-3 those routes were {@code permitAll()} — anyone on the network
 * could fabricate punches. This filter requires a configurable header
 * ({@code X-Device-Api-Key} by default) whose value matches a secret stored
 * in {@code system_config.attendance.device.api_key}.
 * <p>
 * <b>Dev ergonomics:</b> when the configured API key is blank/empty (the
 * default in dev seed data), the filter is a no-op so local development and
 * Swagger calls continue to work. Production deployments MUST set
 * {@code attendance.device.api_key} to a non-empty value via
 * {@code POST /api/admin/system-config} or directly in the {@code system_config}
 * table.
 * <p>
 * Filter ordering: this filter runs BEFORE {@link JwtAuthenticationFilter}
 * (see {@code SecurityConfig.filterChain}) so it can short-circuit the
 * device path without ever consulting the JWT machinery.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceApiKeyFilter extends OncePerRequestFilter {

    /** Endpoints that require a device API key when one is configured. */
    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/api/attendance/punch-in",
            "/api/attendance/punch-out"
    );

    /** Default header name when {@code attendance.device.api_key_header} is missing. */
    private static final String DEFAULT_HEADER = "X-Device-Api-Key";

    private final SystemConfigService systemConfig;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !PROTECTED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String configuredKey = systemConfig.getOrDefault(
                SystemConfigService.Keys.ATTENDANCE_DEVICE_API_KEY, "");

        // Dev / un-configured mode — no enforcement.
        if (configuredKey == null || configuredKey.isBlank()) {
            log.trace("Device API key is not configured — bypassing device auth on {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        String headerName = systemConfig.getOrDefault(
                SystemConfigService.Keys.ATTENDANCE_DEVICE_API_KEY_HEADER, DEFAULT_HEADER);
        String supplied = request.getHeader(headerName);

        if (supplied == null || !configuredKey.equals(supplied)) {
            log.warn("Rejected punch on {} — missing/invalid device API key (header={})",
                    request.getRequestURI(), headerName);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Invalid or missing device API key\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
