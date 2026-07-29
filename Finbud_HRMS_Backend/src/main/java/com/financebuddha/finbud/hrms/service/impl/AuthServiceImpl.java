package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.auth.AdminCreateUserRequest;
import com.financebuddha.finbud.hrms.dto.auth.LoginRequest;
import com.financebuddha.finbud.hrms.dto.auth.LoginResponse;
import com.financebuddha.finbud.hrms.dto.auth.PasswordChangeRequest;
import com.financebuddha.finbud.hrms.dto.auth.RegisterRequest;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.Role;
import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.enums.RoleType;
import com.financebuddha.finbud.hrms.exception.BadRequestException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.exception.UnauthorizedException;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.RoleRepository;
import com.financebuddha.finbud.hrms.repository.UserRepository;
import com.financebuddha.finbud.hrms.security.JwtTokenProvider;
import com.financebuddha.finbud.hrms.service.AuthService;
import com.financebuddha.finbud.hrms.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final SystemConfigService systemConfig;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        try {
            String normalizedUsername = request.getUsername().trim().toLowerCase();
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedUsername, request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByUsername(normalizedUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", normalizedUsername));

            user.setLastLoginAt(LocalDateTime.now());
            user.setFailedLoginAttempts(0);
            userRepository.save(user);

            String accessToken = tokenProvider.generateToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(user.getId());

            List<String> roles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList());

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(86400L)
                    .username(user.getUsername())
                    .employeeId(user.getEmployee() != null ? user.getEmployee().getEmployeeId() : null)
                    .email(user.getEmployee() != null ? user.getEmployee().getEmail() : null)
                    .fullName(user.getEmployee() != null ? user.getEmployee().getFullName() : null)
                    .roles(roles)
                    .mustChangePassword(user.getPasswordChangedAt() == null)
                    .build();

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for user: {}", request.getUsername());
            throw new UnauthorizedException("Invalid username or password");
        }
    }

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }

        Employee employee = employeeRepository.findByEmployeeId(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "employeeId", request.getEmployeeId()));

        if (userRepository.existsByEmployeeId(employee.getId())) {
            throw new BadRequestException("User already exists for this employee");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmployee(employee);
        user.setIsActive(true);

        // ---------------------------------------------------------------
        // C-1 security fix: public /register endpoint must NEVER honour the
        // roles field in the request body. Allowing it lets any anonymous
        // caller self-assign ROLE_ADMIN. Role upgrades go through
        // /api/auth/admin/create-user, which is guarded by @PreAuthorize.
        // ---------------------------------------------------------------
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            log.warn("Ignoring roles={} in public register() call for username={} — /register always grants ROLE_EMPLOYEE",
                    request.getRoles(), request.getUsername());
        }
        Set<Role> roles = new HashSet<>();
        Role employeeRole = roleRepository.findByName(RoleType.ROLE_EMPLOYEE)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "ROLE_EMPLOYEE"));
        roles.add(employeeRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                request.getUsername(), request.getPassword()
        );
        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(savedUser.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .username(savedUser.getUsername())
                .employeeId(employee.getEmployeeId())
                .email(employee.getEmail())
                .fullName(employee.getFullName())
                .roles(roles.stream().map(r -> r.getName().name()).collect(Collectors.toList()))
                // Self-registered users just set their password — no rotation needed.
                .mustChangePassword(Boolean.FALSE)
                .build();
    }

    /**
     * C-1 admin path: provisioning a User on behalf of an existing Employee.
     * Caller must be ROLE_ADMIN — enforced at the controller boundary by
     * {@code @PreAuthorize("hasRole('ADMIN')")} AND by the
     * {@code /api/admin/**} request matcher in SecurityConfig.
     */
    @Override
    @Transactional
    public LoginResponse adminCreateUser(AdminCreateUserRequest request) {
        log.info("Admin provisioning user for employeeId={}", request.getEmployeeId());

        Employee employee = employeeRepository.findByEmployeeId(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "employeeId", request.getEmployeeId()));

        if (userRepository.existsByEmployeeId(employee.getId())) {
            throw new BadRequestException("User already exists for employeeId=" + request.getEmployeeId());
        }

        // Username — explicit > employee.loginUsername > employeeId
        String username = request.getUsername() != null && !request.getUsername().isBlank()
                ? request.getUsername().trim()
                : (employee.getLoginUsername() != null && !employee.getLoginUsername().isBlank()
                        ? employee.getLoginUsername()
                        : employee.getEmployeeId().toLowerCase());

        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username '" + username + "' is already taken");
        }

        // Password — explicit > system_config.auth.default_password > "Welcome@123"
        String rawPassword = request.getPassword() != null && !request.getPassword().isBlank()
                ? request.getPassword()
                : systemConfig.getOrDefault(SystemConfigService.Keys.AUTH_DEFAULT_PASSWORD, "Welcome@123");

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setEmployee(employee);
        user.setIsActive(true);
        // mustChangePassword is honoured via the existing passwordChangedAt
        // contract: leave it null on creation so the front-end can detect a
        // first-login state and force a password reset.
        if (Boolean.FALSE.equals(request.getMustChangePassword())) {
            user.setPasswordChangedAt(LocalDateTime.now());
        }

        // Roles — resolve names with the ROLE_ prefix tolerance.
        Set<Role> grantedRoles = resolveRoles(request.getRoles());
        if (grantedRoles.isEmpty()) {
            String defaultRoleName = systemConfig.getOrDefault(
                    SystemConfigService.Keys.AUTH_DEFAULT_ROLE, RoleType.ROLE_EMPLOYEE.name());
            RoleType resolvedDefault;
            try {
                resolvedDefault = RoleType.valueOf(defaultRoleName);
            } catch (IllegalArgumentException ex) {
                log.warn("Configured default role '{}' is invalid; falling back to ROLE_EMPLOYEE", defaultRoleName);
                resolvedDefault = RoleType.ROLE_EMPLOYEE;
            }
            // `resolvedDefault` is reassigned in the catch branch, so it's not
            // effectively final and cannot be captured directly by a lambda.
            // Bind a final copy for the orElseThrow closure.
            final RoleType defaultRole = resolvedDefault;
            Role role = roleRepository.findByName(defaultRole)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", defaultRole.name()));
            grantedRoles.add(role);
        }
        user.setRoles(grantedRoles);

        User savedUser = userRepository.save(user);
        log.info("Admin created user '{}' with roles={} for employee={}",
                username,
                grantedRoles.stream().map(r -> r.getName().name()).collect(Collectors.toList()),
                employee.getEmployeeId());

        // Tokens are intentionally NOT generated here — the new account must
        // log in to start a session. We return a LoginResponse-shaped payload
        // so the admin UI can show the provisioning summary uniformly.
        return LoginResponse.builder()
                .accessToken(null)
                .refreshToken(null)
                .tokenType("Bearer")
                .expiresIn(0L)
                .username(savedUser.getUsername())
                .employeeId(employee.getEmployeeId())
                .email(employee.getEmail())
                .fullName(employee.getFullName())
                .roles(grantedRoles.stream().map(r -> r.getName().name()).collect(Collectors.toList()))
                // Admin-provisioned accounts must rotate on first login unless explicitly opted out.
                .mustChangePassword(savedUser.getPasswordChangedAt() == null)
                .build();
    }

    /** Tolerant role-name resolver: accepts "ADMIN", "ROLE_ADMIN", "admin". Unknown names are dropped with a warning. */
    private Set<Role> resolveRoles(Set<String> rawNames) {
        Set<Role> resolved = new HashSet<>();
        if (rawNames == null || rawNames.isEmpty()) return resolved;

        for (String raw : rawNames) {
            if (raw == null || raw.isBlank()) continue;
            String upper = raw.trim().toUpperCase();
            if (!upper.startsWith("ROLE_")) upper = "ROLE_" + upper;
            try {
                RoleType type = RoleType.valueOf(upper);
                roleRepository.findByName(type).ifPresentOrElse(
                        resolved::add,
                        () -> log.warn("Role {} not present in DB — skipping (check Flyway seed data)", type));
            } catch (IllegalArgumentException ex) {
                log.warn("Ignoring unknown role name '{}'", raw);
            }
        }
        return resolved;
    }

    @Override
    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        log.info("Changing password for user: {}", userId);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void logout(Long userId) {
        log.info("Logging out user: {}", userId);
        SecurityContextHolder.clearContext();
    }

    @Override
    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        Long userId = tokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        String newAccessToken = tokenProvider.generateTokenFromUserId(user.getId(),
                user.getEmployee() != null ? user.getEmployee().getEmployeeId() : null, roles);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .username(user.getUsername())
                .employeeId(user.getEmployee() != null ? user.getEmployee().getEmployeeId() : null)
                .email(user.getEmployee() != null ? user.getEmployee().getEmail() : null)
                .fullName(user.getEmployee() != null ? user.getEmployee().getFullName() : null)
                .roles(roles)
                .mustChangePassword(user.getPasswordChangedAt() == null)
                .build();
    }
}
