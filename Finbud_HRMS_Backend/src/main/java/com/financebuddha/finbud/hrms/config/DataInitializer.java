package com.financebuddha.finbud.hrms.config;

import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.Role;
import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.enums.EmployeeStatus;
import com.financebuddha.finbud.hrms.enums.EmploymentType;
import com.financebuddha.finbud.hrms.enums.Gender;
import com.financebuddha.finbud.hrms.enums.RoleType;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.RoleRepository;
import com.financebuddha.finbud.hrms.repository.UserRepository;
import com.financebuddha.finbud.hrms.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Bootstrap seeder.
 * <p>
 * Responsibilities on every boot:
 * <ol>
 *   <li>Ensure all four RoleType rows exist (with default permission sets)</li>
 *   <li>Ensure the two Finbud bootstrap admin/HR accounts exist:
 *       <ul>
 *         <li>{@code ND33004} — AKASH DEEP → {@link RoleType#ROLE_ADMIN}</li>
 *         <li>{@code ND33301} — ANJALI BISHT → {@link RoleType#ROLE_HR}</li>
 *       </ul>
 *       Both are created with the system-configured default password
 *       (seeded to {@code finbud@123} via Flyway V7) and
 *       {@code passwordChangedAt = null} so that the first login forces a
 *       rotation (see {@code mustChangePassword} in LoginResponse).
 *   </li>
 * </ol>
 * Everyone else — the ~110 remaining employees on the Noida master sheet —
 * is provisioned by the Excel import flow
 * ({@code POST /api/admin/import/employees}) after the admin logs in for
 * the first time. The importer creates {@link RoleType#ROLE_EMPLOYEE}
 * accounts and leaves {@code passwordChangedAt} null so every employee is
 * also forced to rotate on first login.
 * <p>
 * This class is idempotent: if ND33004 / ND33301 already exist with the
 * correct role, we leave them alone (passwords may have been rotated,
 * extra roles may have been granted). We only fix missing pieces.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD_FALLBACK = "finbud@123";

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemConfigService systemConfig;

    @Override
    public void run(String... args) {
        log.info("DataInitializer — ensuring roles and Finbud bootstrap accounts");

        // 1. Roles
        for (RoleType roleType : RoleType.values()) {
            if (!roleRepository.existsByName(roleType)) {
                Role role = new Role();
                role.setName(roleType);
                role.setDescription(getRoleDescription(roleType));
                role.setPermissions(getDefaultPermissions(roleType));
                roleRepository.save(role);
                log.info("Created role: {}", roleType);
            }
        }

        // 2. Resolve the default password once per boot
        String defaultPassword = systemConfig.getOrDefault(
                SystemConfigService.Keys.AUTH_DEFAULT_PASSWORD, DEFAULT_PASSWORD_FALLBACK);

        // 3. Bootstrap admin + HR. Anything else is the import's job.
        ensureBootstrapUser(
                "ND33004",
                "nd33004",
                "Akash",
                "Deep",
                "aakashkohli27@gmail.com",
                "Senior Sales Manager",
                Gender.MALE,
                LocalDate.of(2024, 5, 1),
                RoleType.ROLE_ADMIN,
                defaultPassword);

        ensureBootstrapUser(
                "ND33301",
                "nd33301",
                "Anjali",
                "Bisht",
                "anjalibisht.7902@gamil.com",
                "HR",
                Gender.FEMALE,
                LocalDate.of(2025, 7, 14),
                RoleType.ROLE_HR,
                defaultPassword);

        // 4. Deactivate the pre-Finbud placeholder users that earlier boots may
        //    have created (usernames "admin" / "hr" / "employee"). Leaving them
        //    enabled alongside the real ND33004 / ND33301 accounts would defeat
        //    the whole point of rotating to the Finbud codes. We deactivate
        //    rather than delete to avoid tripping foreign-key references
        //    (audit logs, etc.) that may point at those rows.
        for (String legacyUsername : new String[] {"admin", "hr", "employee"}) {
            userRepository.findByUsername(legacyUsername).ifPresent(u -> {
                if (Boolean.TRUE.equals(u.getIsActive())) {
                    u.setIsActive(Boolean.FALSE);
                    userRepository.save(u);
                    log.info("Deactivated legacy placeholder user '{}'", u.getUsername());
                }
            });
        }

        log.info("DataInitializer — done");
    }

    /**
     * Idempotent: ensures the Employee row, then the User row, then the role
     * grant. Will not clobber an existing password — once an admin rotates,
     * the new hash is preserved across restarts.
     */
    private void ensureBootstrapUser(String employeeCode,
                                     String username,
                                     String firstName,
                                     String lastName,
                                     String email,
                                     String designation,
                                     Gender gender,
                                     LocalDate joiningDate,
                                     RoleType roleType,
                                     String defaultPassword) {

        // Employee side
        Employee employee = employeeRepository.findByEmployeeId(employeeCode)
                .orElseGet(() -> {
                    Employee e = new Employee();
                    e.setEmployeeId(employeeCode);
                    e.setFirstName(firstName);
                    e.setLastName(lastName);
                    e.setEmail(email);
                    e.setDesignation(designation);
                    e.setGender(gender);
                    e.setDateOfJoining(joiningDate);
                    e.setEmploymentType(EmploymentType.FULL_TIME);
                    e.setStatus(EmployeeStatus.ACTIVE);
                    e.setLoginUsername(username);
                    Employee saved = employeeRepository.save(e);
                    log.info("Bootstrap: created Employee {}", employeeCode);
                    return saved;
                });

        // User side
        Optional<User> existing = userRepository.findByUsername(username);
        User user;
        if (existing.isPresent()) {
            user = existing.get();
            // Don't rewrite the password — admin may already have rotated.
            // Do ensure the account is active and associated with the right employee.
            boolean dirty = false;
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                user.setIsActive(Boolean.TRUE);
                dirty = true;
            }
            if (user.getEmployee() == null) {
                user.setEmployee(employee);
                dirty = true;
            }
            if (dirty) {
                userRepository.save(user);
                log.info("Bootstrap: refreshed User {}", username);
            }
        } else {
            user = User.builder()
                    .username(username)
                    .passwordHash(passwordEncoder.encode(defaultPassword))
                    .employee(employee)
                    .isActive(Boolean.TRUE)
                    // passwordChangedAt intentionally left null — forces first-login rotation.
                    .build();
            user = userRepository.save(user);
            log.info("Bootstrap: created User {} (employee {}) with role {} and default password — rotation forced on first login",
                    username, employeeCode, roleType);
        }

        // Role side — grant if missing
        boolean hasRole = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> r.getName() == roleType);
        if (!hasRole) {
            Role role = roleRepository.findByName(roleType)
                    .orElseThrow(() -> new IllegalStateException(
                            "Role " + roleType + " missing — DataInitializer should have created it"));
            if (user.getRoles() == null) {
                user.setRoles(new HashSet<>());
            }
            user.getRoles().add(role);
            userRepository.save(user);
            log.info("Bootstrap: granted {} to {}", roleType, username);
        }
    }

    private String getRoleDescription(RoleType roleType) {
        return switch (roleType) {
            case ROLE_ADMIN -> "System administrator with full access";
            case ROLE_HR -> "Human resources manager";
            case ROLE_MANAGER -> "Team manager with department access";
            case ROLE_EMPLOYEE -> "Regular employee";
        };
    }

    private Set<String> getDefaultPermissions(RoleType roleType) {
        Set<String> permissions = new HashSet<>();
        switch (roleType) {
            case ROLE_ADMIN:
                permissions.addAll(Set.of("*"));
                break;
            case ROLE_HR:
                permissions.addAll(Set.of("employee:read", "employee:write", "employee:delete",
                        "payroll:read", "payroll:write", "attendance:read", "leave:approve"));
                break;
            case ROLE_MANAGER:
                permissions.addAll(Set.of("employee:read", "attendance:read", "leave:approve"));
                break;
            case ROLE_EMPLOYEE:
                permissions.addAll(Set.of("employee:read", "attendance:read", "attendance:write",
                        "leave:read", "leave:write", "payroll:read"));
                break;
        }
        return permissions;
    }
}
