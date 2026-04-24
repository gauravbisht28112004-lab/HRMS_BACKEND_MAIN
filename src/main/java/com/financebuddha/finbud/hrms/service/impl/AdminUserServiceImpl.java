package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.auth.PasswordResetResponse;
import com.financebuddha.finbud.hrms.dto.auth.UpdateUserRolesRequest;
import com.financebuddha.finbud.hrms.dto.auth.UpdateUserStatusRequest;
import com.financebuddha.finbud.hrms.dto.auth.UserAccountResponse;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.Role;
import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.enums.RoleType;
import com.financebuddha.finbud.hrms.exception.BadRequestException;
import com.financebuddha.finbud.hrms.exception.ForbiddenException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.RoleRepository;
import com.financebuddha.finbud.hrms.repository.UserRepository;
import com.financebuddha.finbud.hrms.service.AdminUserService;
import com.financebuddha.finbud.hrms.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * See {@link AdminUserService} for the authorisation contract. The key
 * invariant is that HR-only sessions cannot promote someone into (or demote
 * someone out of) {@code ROLE_ADMIN} / {@code ROLE_HR} — we check that on
 * the server regardless of what the client sent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemConfigService systemConfig;

    /** Roles HR alone may grant or revoke. Admin can touch anything. */
    private static final Set<RoleType> HR_ALLOWED_ROLES = EnumSet.of(
            RoleType.ROLE_EMPLOYEE, RoleType.ROLE_MANAGER);

    /** Fallback when {@code system_config.auth.default_password} is unset. Kept in sync with V7 Flyway. */
    private static final String FALLBACK_DEFAULT_PASSWORD = "finbud@123";

    @Override
    @Transactional(readOnly = true)
    public UserAccountResponse getByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAccountResponse getByEmployeeCode(String employeeCode) {
        Employee employee = employeeRepository.findByEmployeeId(employeeCode)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "employeeId", employeeCode));

        User user = userRepository.findByEmployeeId(employee.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "employeeId", employeeCode));

        return toResponse(user);
    }

    @Override
    @Transactional
    public UserAccountResponse updateRoles(Long userId, UpdateUserRolesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Set<RoleType> requestedTypes = normaliseRoleNames(request.getRoles());
        if (requestedTypes.isEmpty()) {
            throw new BadRequestException("No valid roles supplied");
        }

        boolean callerIsAdmin = currentCallerHasRole(RoleType.ROLE_ADMIN);

        // HR-only callers may never introduce or remove privileged roles.
        if (!callerIsAdmin) {
            Set<RoleType> existing = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(RoleType.class)));

            Set<RoleType> beingGranted = EnumSet.copyOf(requestedTypes);
            beingGranted.removeAll(existing);

            Set<RoleType> beingRevoked = EnumSet.copyOf(existing);
            beingRevoked.removeAll(requestedTypes);

            Set<RoleType> touched = EnumSet.copyOf(beingGranted);
            touched.addAll(beingRevoked);

            for (RoleType t : touched) {
                if (!HR_ALLOWED_ROLES.contains(t)) {
                    throw new ForbiddenException(
                            "HR cannot grant or revoke " + t.name()
                                    + " — only an Admin may modify privileged roles");
                }
            }
        }

        // Hydrate DB rows for the requested types.
        Set<Role> resolvedRoles = new HashSet<>();
        for (RoleType t : requestedTypes) {
            Role role = roleRepository.findByName(t)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", t.name()));
            resolvedRoles.add(role);
        }
        user.setRoles(resolvedRoles);

        User saved = userRepository.save(user);
        log.info("User {} roles updated to {} by caller with admin={}",
                user.getUsername(),
                requestedTypes.stream().map(Enum::name).toList(),
                callerIsAdmin);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public UserAccountResponse updateStatus(Long userId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        boolean callerIsAdmin = currentCallerHasRole(RoleType.ROLE_ADMIN);

        // HR may not toggle admin accounts — admins are managed by admins.
        if (!callerIsAdmin) {
            boolean userIsAdmin = user.getRoles().stream()
                    .anyMatch(r -> r.getName() == RoleType.ROLE_ADMIN
                                || r.getName() == RoleType.ROLE_HR);
            if (userIsAdmin) {
                throw new ForbiddenException(
                        "HR cannot change the status of an Admin or HR account");
            }
        }

        user.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
        // Deactivation should also drop any lockout so reactivation is clean.
        if (Boolean.TRUE.equals(user.getIsActive())) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
        }

        User saved = userRepository.save(user);
        log.info("User {} isActive set to {} by caller with admin={}",
                user.getUsername(), user.getIsActive(), callerIsAdmin);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PasswordResetResponse resetPassword(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        boolean callerIsAdmin = currentCallerHasRole(RoleType.ROLE_ADMIN);
        if (!callerIsAdmin) {
            boolean userIsPrivileged = user.getRoles().stream()
                    .anyMatch(r -> r.getName() == RoleType.ROLE_ADMIN
                                || r.getName() == RoleType.ROLE_HR);
            if (userIsPrivileged) {
                throw new ForbiddenException(
                        "HR cannot reset the password of an Admin or HR account");
            }
        }

        String rawPassword = systemConfig.getOrDefault(
                SystemConfigService.Keys.AUTH_DEFAULT_PASSWORD, FALLBACK_DEFAULT_PASSWORD);

        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        // Null passwordChangedAt ⇒ mustChangePassword=true on next login.
        user.setPasswordChangedAt(null);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        log.info("Password reset to default for user '{}' by caller with admin={}",
                user.getUsername(), callerIsAdmin);

        return PasswordResetResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .temporaryPassword(rawPassword)
                .mustChangePassword(Boolean.TRUE)
                .build();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private UserAccountResponse toResponse(User user) {
        Employee employee = user.getEmployee();

        boolean locked = user.getLockedUntil() != null
                && user.getLockedUntil().isAfter(LocalDateTime.now());

        return UserAccountResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .employeeRowId(employee != null ? employee.getId() : null)
                .employeeId(employee != null ? employee.getEmployeeId() : null)
                .fullName(employee != null ? employee.getFullName() : null)
                .email(employee != null ? employee.getEmail() : null)
                .isActive(user.getIsActive())
                .roles(user.getRoles().stream()
                        .map(r -> r.getName().name())
                        .collect(Collectors.toCollection(HashSet::new)))
                .lastLoginAt(user.getLastLoginAt())
                .passwordChangedAt(user.getPasswordChangedAt())
                .mustChangePassword(user.getPasswordChangedAt() == null)
                .locked(locked)
                .lockedUntil(user.getLockedUntil())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .build();
    }

    /**
     * Converts arbitrary role strings (e.g. "admin", "ROLE_HR", "Manager")
     * into a set of {@link RoleType}. Unknown names are dropped with a
     * warning rather than failing the whole call — matches the tolerant
     * behaviour of {@link AuthServiceImpl#resolveRoles}.
     */
    private Set<RoleType> normaliseRoleNames(Set<String> rawNames) {
        Set<RoleType> out = EnumSet.noneOf(RoleType.class);
        if (rawNames == null) return out;
        for (String raw : rawNames) {
            if (raw == null || raw.isBlank()) continue;
            String upper = raw.trim().toUpperCase();
            if (!upper.startsWith("ROLE_")) upper = "ROLE_" + upper;
            try {
                out.add(RoleType.valueOf(upper));
            } catch (IllegalArgumentException ex) {
                log.warn("Ignoring unknown role name '{}' in role update request", raw);
            }
        }
        return out;
    }

    /** Does the currently authenticated caller hold the given role? */
    private boolean currentCallerHasRole(RoleType role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        String target = role.name();
        for (GrantedAuthority granted : auth.getAuthorities()) {
            if (target.equals(granted.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
