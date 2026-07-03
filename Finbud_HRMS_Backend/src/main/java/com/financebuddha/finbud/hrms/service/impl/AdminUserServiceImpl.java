package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.auth.BulkPasswordResetResponse;
import com.financebuddha.finbud.hrms.dto.auth.PasswordResetResponse;
import com.financebuddha.finbud.hrms.dto.auth.ProvisionMissingUsersResponse;
import com.financebuddha.finbud.hrms.dto.auth.UpdateUserRolesRequest;
import com.financebuddha.finbud.hrms.dto.auth.UpdateUserStatusRequest;
import com.financebuddha.finbud.hrms.dto.auth.UserAccountResponse;
import com.financebuddha.finbud.hrms.dto.auth.UserLoginDebugResponse;
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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
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
            RoleType.ROLE_EMPLOYEE, RoleType.ROLE_MANAGER, RoleType.ROLE_ATL);

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

    @Override
    @Transactional(readOnly = true)
    public UserLoginDebugResponse debugLogin(String username) {
        if (username == null || username.isBlank()) {
            return UserLoginDebugResponse.builder()
                    .username(username)
                    .exists(false)
                    .loginDiagnosis("No username provided.")
                    .suggestedAction("NONE")
                    .build();
        }

        // Usernames are stored lowercase by the importer; normalise so the
        // diagnostic matches what's actually in the DB no matter how the
        // HR operator typed it into the admin UI.
        String normalised = username.trim().toLowerCase();
        User user = userRepository.findByUsername(normalised).orElse(null);

        if (user == null) {
            return UserLoginDebugResponse.builder()
                    .username(normalised)
                    .exists(false)
                    .loginDiagnosis("No user row with username '" + normalised
                            + "'. The employee may not have been provisioned. "
                            + "If the employee exists, open their profile and click 'Provision login'.")
                    .suggestedAction("PROVISION_LOGIN")
                    .build();
        }

        // Compute lock state with a fresh wall-clock read — locked_until may
        // already have expired, in which case we shouldn't report "locked".
        LocalDateTime now = LocalDateTime.now();
        boolean locked = user.getLockedUntil() != null && user.getLockedUntil().isAfter(now);
        boolean neverLoggedIn = user.getPasswordChangedAt() == null;

        String diagnosis;
        String action;
        if (Boolean.FALSE.equals(user.getIsActive())) {
            diagnosis = "Account is inactive — Spring Security rejects login with no visible reason. Activate it and retry.";
            action = "ACTIVATE_ACCOUNT";
        } else if (locked) {
            diagnosis = "Account is locked until " + user.getLockedUntil() + ". Reset password to unlock and clear failed attempts.";
            action = "UNLOCK_ACCOUNT";
        } else if (neverLoggedIn) {
            diagnosis = "User exists and is active, but has never logged in — they are still on the temp / default password "
                    + "from the last provision/reset. If the original temp password was lost, click 'Reset password' to generate a fresh one.";
            action = "RESET_PASSWORD";
        } else {
            diagnosis = "Account is active, unlocked, and has logged in before. Login failure is almost certainly a wrong password. "
                    + "Use 'Reset password' to issue a fresh temp password.";
            action = "RESET_PASSWORD";
        }

        Set<String> roleNames = user.getRoles() == null ? Set.of()
                : user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet());

        return UserLoginDebugResponse.builder()
                .username(user.getUsername())
                .exists(true)
                .userId(user.getId())
                .isActive(user.getIsActive())
                .roles(roleNames)
                .lastLoginAt(user.getLastLoginAt())
                .passwordChangedAt(user.getPasswordChangedAt())
                .neverLoggedIn(neverLoggedIn)
                .locked(locked)
                .lockedUntil(user.getLockedUntil())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .employeeCode(user.getEmployee() != null ? user.getEmployee().getEmployeeId() : null)
                .fullName(user.getEmployee() != null ? user.getEmployee().getFullName() : null)
                .loginDiagnosis(diagnosis)
                .suggestedAction(action)
                .build();
    }

    @Override
    @Transactional
    public BulkPasswordResetResponse bulkResetUntouchedAccounts() {
        // Admin-only check is also enforced at the controller layer; we
        // double-check here so the service is safe to call from tests /
        // future internal jobs without a request context.
        if (!currentCallerHasRole(RoleType.ROLE_ADMIN)) {
            throw new ForbiddenException("Only Admin can bulk-reset passwords");
        }

        String defaultPassword = systemConfig.getOrDefault(
                SystemConfigService.Keys.AUTH_DEFAULT_PASSWORD, FALLBACK_DEFAULT_PASSWORD);
        String hash = passwordEncoder.encode(defaultPassword);

        List<User> untouched = userRepository.findByPasswordChangedAtIsNull();

        // We exclude Admin/HR seed accounts from the bulk reset — those are
        // touched by DataInitializer at boot and shouldn't be re-stamped
        // here. If an admin really wants to reset themselves, they can use
        // the per-user reset endpoint.
        List<String> reset = new ArrayList<>();
        int skipped = 0;
        for (User user : untouched) {
            boolean isPrivileged = user.getRoles() != null && user.getRoles().stream()
                    .anyMatch(r -> r.getName() == RoleType.ROLE_ADMIN
                                || r.getName() == RoleType.ROLE_HR);
            if (isPrivileged) {
                skipped++;
                continue;
            }
            user.setPasswordHash(hash);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            // passwordChangedAt stays null so they're forced to rotate on
            // first login (mustChangePassword=true).
            reset.add(user.getUsername());
        }
        userRepository.saveAll(untouched);

        log.info("Bulk password reset: {} accounts reset to default, {} privileged accounts skipped",
                reset.size(), skipped);

        return BulkPasswordResetResponse.builder()
                .resetCount(reset.size())
                .skippedCount(skipped)
                .defaultPassword(defaultPassword)
                .resetUsernames(reset)
                .build();
    }

    @Override
    @Transactional
    public ProvisionMissingUsersResponse provisionMissingUsers() {
        if (!currentCallerHasRole(RoleType.ROLE_ADMIN)) {
            throw new ForbiddenException("Only Admin can provision missing user accounts");
        }

        String defaultPassword = systemConfig.getOrDefault(
                SystemConfigService.Keys.AUTH_DEFAULT_PASSWORD, FALLBACK_DEFAULT_PASSWORD);
        String passwordHash = passwordEncoder.encode(defaultPassword);

        Role employeeRole = roleRepository.findByName(RoleType.ROLE_EMPLOYEE)
                .orElseThrow(() -> new IllegalStateException("ROLE_EMPLOYEE missing — check Flyway seed data"));

        List<Employee> unprovisioned = employeeRepository.findAllWithoutUserAccount();

        List<String> provisioned = new ArrayList<>();
        List<ProvisionMissingUsersResponse.FailureDetail> failures = new ArrayList<>();

        for (Employee employee : unprovisioned) {
            String username = (employee.getLoginUsername() != null && !employee.getLoginUsername().isBlank())
                    ? employee.getLoginUsername().trim().toLowerCase()
                    : employee.getEmployeeId().toLowerCase();

            try {
                if (userRepository.existsByUsername(username)) {
                    failures.add(ProvisionMissingUsersResponse.FailureDetail.builder()
                            .employeeId(employee.getEmployeeId())
                            .reason("Username '" + username + "' already taken by another user")
                            .build());
                    log.warn("Skipped provisioning for {} — username '{}' already taken",
                            employee.getEmployeeId(), username);
                    continue;
                }

                Set<Role> roles = new HashSet<>();
                roles.add(employeeRole);

                User user = User.builder()
                        .username(username)
                        .passwordHash(passwordHash)
                        .employee(employee)
                        .isActive(Boolean.TRUE)
                        .roles(roles)
                        // passwordChangedAt intentionally null → forces rotation on first login
                        .build();

                userRepository.save(user);
                provisioned.add(username);
                log.info("Provisioned login '{}' for employee {}", username, employee.getEmployeeId());

            } catch (Exception e) {
                log.error("Failed to provision user for employee {}: {}", employee.getEmployeeId(), e.getMessage(), e);
                failures.add(ProvisionMissingUsersResponse.FailureDetail.builder()
                        .employeeId(employee.getEmployeeId())
                        .reason(e.getMessage())
                        .build());
            }
        }

        log.info("provisionMissingUsers complete — provisioned={}, failed={}",
                provisioned.size(), failures.size());

        return ProvisionMissingUsersResponse.builder()
                .provisionedCount(provisioned.size())
                .alreadyProvisionedCount(0) // query only returns employees with no user row
                .failedCount(failures.size())
                .defaultPassword(defaultPassword)
                .provisionedUsernames(provisioned)
                .failures(failures)
                .build();
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
