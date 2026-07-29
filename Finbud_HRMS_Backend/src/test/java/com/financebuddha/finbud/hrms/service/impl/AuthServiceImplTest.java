package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.auth.AdminCreateUserRequest;
import com.financebuddha.finbud.hrms.dto.auth.LoginResponse;
import com.financebuddha.finbud.hrms.dto.auth.RegisterRequest;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.Role;
import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.enums.RoleType;
import com.financebuddha.finbud.hrms.exception.BadRequestException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.RoleRepository;
import com.financebuddha.finbud.hrms.repository.UserRepository;
import com.financebuddha.finbud.hrms.security.JwtTokenProvider;
import com.financebuddha.finbud.hrms.service.SystemConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthServiceImpl}. The core invariants verified here:
 * <ul>
 *   <li><b>C-1:</b> {@code register()} ALWAYS grants {@code ROLE_EMPLOYEE}
 *       regardless of the {@code roles} field in the request body. This is
 *       non-negotiable — the public endpoint is unauthenticated.</li>
 *   <li>{@code adminCreateUser()} honours explicit roles (with "ROLE_" prefix
 *       tolerance), falls back to the system-config default password, and
 *       derives a username when one isn't supplied.</li>
 *   <li>Unknown role names are dropped with a warning rather than throwing.</li>
 * </ul>
 *
 * <p>We disable Mockito strict stubs here — {@link AuthServiceImpl#register}
 * has a long happy-path that stubs more collaborators than any one test can
 * exercise.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SystemConfigService systemConfig;

    @InjectMocks
    private AuthServiceImpl service;

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private Employee sampleEmployee() {
        Employee e = new Employee();
        e.setId(42L);
        e.setEmployeeId("ND12345");
        e.setFirstName("Test");
        e.setLastName("User");
        e.setEmail("test.user@finbud.in");
        return e;
    }

    private Role role(RoleType type) {
        Role r = new Role();
        r.setId((long) type.ordinal() + 1);
        r.setName(type);
        return r;
    }

    private RegisterRequest registerReq(String username, String employeeId, Set<String> roles) {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setEmployeeId(employeeId);
        req.setEmail("test.user@finbud.in");
        req.setPassword("Password123");
        req.setRoles(roles);
        return req;
    }

    // ------------------------------------------------------------------
    // register()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("register() — public self-service")
    class RegisterTests {

        @Test
        @DisplayName("C-1: roles in the request body are IGNORED; ROLE_EMPLOYEE is always granted")
        void registerAlwaysGrantsEmployeeRole() {
            RegisterRequest req = registerReq("jdoe", "ND12345",
                    Set.of("ROLE_ADMIN", "ROLE_HR")); // hostile payload trying to self-assign

            when(userRepository.existsByUsername("jdoe")).thenReturn(false);
            when(employeeRepository.findByEmployeeId("ND12345")).thenReturn(Optional.of(sampleEmployee()));
            when(userRepository.existsByEmployeeId(42L)).thenReturn(false);
            when(passwordEncoder.encode("Password123")).thenReturn("$bcrypt");
            when(roleRepository.findByName(RoleType.ROLE_EMPLOYEE))
                    .thenReturn(Optional.of(role(RoleType.ROLE_EMPLOYEE)));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(99L);           // real DB-generated id, required for tokenProvider
                return u;
            });
            when(tokenProvider.generateToken(any())).thenReturn("jwt-access");
            when(tokenProvider.generateRefreshToken(99L)).thenReturn("jwt-refresh");

            LoginResponse response = service.register(req);

            // Captured user should have exactly one role: ROLE_EMPLOYEE.
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            Set<RoleType> grantedTypes = new HashSet<>();
            captor.getValue().getRoles().forEach(r -> grantedTypes.add(r.getName()));

            assertThat(grantedTypes).containsExactly(RoleType.ROLE_EMPLOYEE);
            assertThat(grantedTypes).doesNotContain(RoleType.ROLE_ADMIN, RoleType.ROLE_HR);
            assertThat(response.getRoles()).containsExactly("ROLE_EMPLOYEE");
        }

        @Test
        @DisplayName("register() hashes the password — raw password never stored")
        void registerHashesPassword() {
            RegisterRequest req = registerReq("jdoe", "ND12345", null);

            when(userRepository.existsByUsername("jdoe")).thenReturn(false);
            when(employeeRepository.findByEmployeeId("ND12345")).thenReturn(Optional.of(sampleEmployee()));
            when(userRepository.existsByEmployeeId(42L)).thenReturn(false);
            when(passwordEncoder.encode("Password123")).thenReturn("HASHED");
            when(roleRepository.findByName(RoleType.ROLE_EMPLOYEE))
                    .thenReturn(Optional.of(role(RoleType.ROLE_EMPLOYEE)));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(99L);
                return u;
            });

            service.register(req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("HASHED");
            assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("Password123");
        }

        @Test
        @DisplayName("Duplicate username is rejected with BadRequestException")
        void duplicateUsernameRejected() {
            RegisterRequest req = registerReq("jdoe", "ND12345", null);
            when(userRepository.existsByUsername("jdoe")).thenReturn(true);

            assertThatThrownBy(() -> service.register(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Username already exists");
        }

        @Test
        @DisplayName("Missing employee is rejected with ResourceNotFoundException")
        void missingEmployeeRejected() {
            RegisterRequest req = registerReq("jdoe", "ND99999", null);
            when(userRepository.existsByUsername("jdoe")).thenReturn(false);
            when(employeeRepository.findByEmployeeId("ND99999")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.register(req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Employee that already has a user is rejected")
        void employeeAlreadyLinked() {
            RegisterRequest req = registerReq("jdoe", "ND12345", null);
            when(userRepository.existsByUsername("jdoe")).thenReturn(false);
            when(employeeRepository.findByEmployeeId("ND12345")).thenReturn(Optional.of(sampleEmployee()));
            when(userRepository.existsByEmployeeId(42L)).thenReturn(true);

            assertThatThrownBy(() -> service.register(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("User already exists");
        }
    }

    // ------------------------------------------------------------------
    // adminCreateUser()
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("adminCreateUser() — admin-only provisioning")
    class AdminCreateUserTests {

        private AdminCreateUserRequest adminReq() {
            AdminCreateUserRequest r = new AdminCreateUserRequest();
            r.setEmployeeId("ND12345");
            return r;
        }

        @Test
        @DisplayName("Explicit roles are granted; bare names are promoted to ROLE_* form")
        void explicitRolesGranted() {
            AdminCreateUserRequest req = adminReq();
            req.setUsername("alice");
            req.setPassword("Str0ngPass!");
            req.setRoles(Set.of("HR", "manager"));   // one bare, one lowercase

            when(employeeRepository.findByEmployeeId("ND12345")).thenReturn(Optional.of(sampleEmployee()));
            when(userRepository.existsByEmployeeId(42L)).thenReturn(false);
            when(userRepository.existsByUsername("alice")).thenReturn(false);
            when(passwordEncoder.encode("Str0ngPass!")).thenReturn("$hashed");
            when(roleRepository.findByName(RoleType.ROLE_HR))
                    .thenReturn(Optional.of(role(RoleType.ROLE_HR)));
            when(roleRepository.findByName(RoleType.ROLE_MANAGER))
                    .thenReturn(Optional.of(role(RoleType.ROLE_MANAGER)));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            LoginResponse resp = service.adminCreateUser(req);

            assertThat(resp.getRoles()).containsExactlyInAnyOrder("ROLE_HR", "ROLE_MANAGER");
            // adminCreateUser intentionally returns null tokens — the provisioned
            // account must log in to start a session.
            assertThat(resp.getAccessToken()).isNull();
            assertThat(resp.getRefreshToken()).isNull();
        }

        @Test
        @DisplayName("Unknown role names are dropped — valid ones are still granted")
        void unknownRoleNamesDropped() {
            AdminCreateUserRequest req = adminReq();
            req.setUsername("alice");
            req.setPassword("Str0ngPass!");
            req.setRoles(Set.of("ADMIN", "NOT_A_ROLE", "whatever"));

            when(employeeRepository.findByEmployeeId("ND12345")).thenReturn(Optional.of(sampleEmployee()));
            when(userRepository.existsByEmployeeId(42L)).thenReturn(false);
            when(userRepository.existsByUsername("alice")).thenReturn(false);
            when(passwordEncoder.encode("Str0ngPass!")).thenReturn("$hashed");
            when(roleRepository.findByName(RoleType.ROLE_ADMIN))
                    .thenReturn(Optional.of(role(RoleType.ROLE_ADMIN)));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            LoginResponse resp = service.adminCreateUser(req);

            assertThat(resp.getRoles()).containsExactly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("When password omitted: service uses system_config default (auth.default_password)")
        void passwordFallbackFromSystemConfig() {
            AdminCreateUserRequest req = adminReq();
            req.setUsername("bob");
            // no password, no roles — should fall back to ROLE_EMPLOYEE default

            when(employeeRepository.findByEmployeeId("ND12345")).thenReturn(Optional.of(sampleEmployee()));
            when(userRepository.existsByEmployeeId(42L)).thenReturn(false);
            when(userRepository.existsByUsername("bob")).thenReturn(false);
            when(systemConfig.getOrDefault(SystemConfigService.Keys.AUTH_DEFAULT_PASSWORD, "Welcome@123"))
                    .thenReturn("FromConfig!2026");
            when(systemConfig.getOrDefault(SystemConfigService.Keys.AUTH_DEFAULT_ROLE, "ROLE_EMPLOYEE"))
                    .thenReturn("ROLE_EMPLOYEE");
            when(passwordEncoder.encode("FromConfig!2026")).thenReturn("$hashedFromConfig");
            when(roleRepository.findByName(RoleType.ROLE_EMPLOYEE))
                    .thenReturn(Optional.of(role(RoleType.ROLE_EMPLOYEE)));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            service.adminCreateUser(req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("$hashedFromConfig");
            // Must have queried the config key — not silently baked in a default.
            verify(passwordEncoder).encode("FromConfig!2026");
        }

        @Test
        @DisplayName("Invalid configured default role falls back to ROLE_EMPLOYEE without throwing")
        void invalidConfiguredDefaultRoleFallsBack() {
            AdminCreateUserRequest req = adminReq();
            req.setUsername("bob");
            req.setPassword("Str0ngPass!");
            // no roles — triggers default-role lookup

            when(employeeRepository.findByEmployeeId("ND12345")).thenReturn(Optional.of(sampleEmployee()));
            when(userRepository.existsByEmployeeId(42L)).thenReturn(false);
            when(userRepository.existsByUsername("bob")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$hashed");
            when(systemConfig.getOrDefault(SystemConfigService.Keys.AUTH_DEFAULT_ROLE, "ROLE_EMPLOYEE"))
                    .thenReturn("ROLE_GARBAGE");       // not a valid RoleType
            when(roleRepository.findByName(RoleType.ROLE_EMPLOYEE))
                    .thenReturn(Optional.of(role(RoleType.ROLE_EMPLOYEE)));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            LoginResponse resp = service.adminCreateUser(req);

            assertThat(resp.getRoles()).containsExactly("ROLE_EMPLOYEE");
        }

        @Test
        @DisplayName("Username is derived from employee.loginUsername when request omits it")
        void usernameDerivedFromLoginUsername() {
            AdminCreateUserRequest req = adminReq();
            req.setPassword("Str0ngPass!");

            Employee e = sampleEmployee();
            e.setLoginUsername("test.user");
            when(employeeRepository.findByEmployeeId("ND12345")).thenReturn(Optional.of(e));
            when(userRepository.existsByEmployeeId(42L)).thenReturn(false);
            when(userRepository.existsByUsername("test.user")).thenReturn(false);
            when(passwordEncoder.encode("Str0ngPass!")).thenReturn("$hashed");
            when(systemConfig.getOrDefault(SystemConfigService.Keys.AUTH_DEFAULT_ROLE, "ROLE_EMPLOYEE"))
                    .thenReturn("ROLE_EMPLOYEE");
            when(roleRepository.findByName(RoleType.ROLE_EMPLOYEE))
                    .thenReturn(Optional.of(role(RoleType.ROLE_EMPLOYEE)));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            LoginResponse resp = service.adminCreateUser(req);
            assertThat(resp.getUsername()).isEqualTo("test.user");
        }

        @Test
        @DisplayName("Username falls back to lowercase employeeId when both explicit username and loginUsername are absent")
        void usernameFallsBackToEmployeeId() {
            AdminCreateUserRequest req = adminReq();
            req.setPassword("Str0ngPass!");

            Employee e = sampleEmployee();   // no loginUsername
            when(employeeRepository.findByEmployeeId("ND12345")).thenReturn(Optional.of(e));
            when(userRepository.existsByEmployeeId(42L)).thenReturn(false);
            when(userRepository.existsByUsername("nd12345")).thenReturn(false);
            when(passwordEncoder.encode("Str0ngPass!")).thenReturn("$hashed");
            when(systemConfig.getOrDefault(SystemConfigService.Keys.AUTH_DEFAULT_ROLE, "ROLE_EMPLOYEE"))
                    .thenReturn("ROLE_EMPLOYEE");
            when(roleRepository.findByName(RoleType.ROLE_EMPLOYEE))
                    .thenReturn(Optional.of(role(RoleType.ROLE_EMPLOYEE)));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            LoginResponse resp = service.adminCreateUser(req);
            assertThat(resp.getUsername()).isEqualTo("nd12345");
        }

        @Test
        @DisplayName("Explicit username beats both loginUsername and employeeId derivation")
        void explicitUsernameBeatsOthers() {
            AdminCreateUserRequest req = adminReq();
            req.setUsername("explicit.name");
            req.setPassword("Str0ngPass!");

            Employee e = sampleEmployee();
            e.setLoginUsername("test.user"); // should be ignored in favour of explicit
            when(employeeRepository.findByEmployeeId("ND12345")).thenReturn(Optional.of(e));
            when(userRepository.existsByEmployeeId(42L)).thenReturn(false);
            when(userRepository.existsByUsername("explicit.name")).thenReturn(false);
            when(passwordEncoder.encode("Str0ngPass!")).thenReturn("$hashed");
            when(systemConfig.getOrDefault(SystemConfigService.Keys.AUTH_DEFAULT_ROLE, "ROLE_EMPLOYEE"))
                    .thenReturn("ROLE_EMPLOYEE");
            when(roleRepository.findByName(RoleType.ROLE_EMPLOYEE))
                    .thenReturn(Optional.of(role(RoleType.ROLE_EMPLOYEE)));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            LoginResponse resp = service.adminCreateUser(req);
            assertThat(resp.getUsername()).isEqualTo("explicit.name");
        }

        @Test
        @DisplayName("Missing employee yields ResourceNotFoundException")
        void missingEmployeeRejected() {
            AdminCreateUserRequest req = adminReq();
            when(employeeRepository.findByEmployeeId("ND12345")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.adminCreateUser(req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Employee already linked to a user is rejected")
        void employeeAlreadyHasUser() {
            AdminCreateUserRequest req = adminReq();
            when(employeeRepository.findByEmployeeId("ND12345")).thenReturn(Optional.of(sampleEmployee()));
            when(userRepository.existsByEmployeeId(42L)).thenReturn(true);

            assertThatThrownBy(() -> service.adminCreateUser(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("User already exists");
        }

        @Test
        @DisplayName("Duplicate username rejected — even on admin path")
        void duplicateUsernameRejected() {
            AdminCreateUserRequest req = adminReq();
            req.setUsername("taken");

            when(employeeRepository.findByEmployeeId("ND12345")).thenReturn(Optional.of(sampleEmployee()));
            when(userRepository.existsByEmployeeId(42L)).thenReturn(false);
            when(userRepository.existsByUsername("taken")).thenReturn(true);

            assertThatThrownBy(() -> service.adminCreateUser(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already taken");
        }

        @Test
        @DisplayName("mustChangePassword = false clears passwordChangedAt; true/null leaves it null")
        void mustChangePasswordControlsPasswordChangedAt() {
            AdminCreateUserRequest req = adminReq();
            req.setUsername("bob");
            req.setPassword("Str0ngPass!");
            req.setMustChangePassword(Boolean.FALSE);

            when(employeeRepository.findByEmployeeId("ND12345")).thenReturn(Optional.of(sampleEmployee()));
            when(userRepository.existsByEmployeeId(42L)).thenReturn(false);
            when(userRepository.existsByUsername("bob")).thenReturn(false);
            when(passwordEncoder.encode("Str0ngPass!")).thenReturn("$hashed");
            when(systemConfig.getOrDefault(SystemConfigService.Keys.AUTH_DEFAULT_ROLE, "ROLE_EMPLOYEE"))
                    .thenReturn("ROLE_EMPLOYEE");
            when(roleRepository.findByName(RoleType.ROLE_EMPLOYEE))
                    .thenReturn(Optional.of(role(RoleType.ROLE_EMPLOYEE)));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            service.adminCreateUser(req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            // passwordChangedAt should be set (not null) because mustChangePassword=false.
            assertThat(captor.getValue().getPasswordChangedAt()).isNotNull();
        }

        @Test
        @DisplayName("mustChangePassword = true (default) leaves passwordChangedAt null — first login forces reset")
        void mustChangePasswordTrueLeavesTimestampNull() {
            AdminCreateUserRequest req = adminReq();
            req.setUsername("bob");
            req.setPassword("Str0ngPass!");
            // mustChangePassword defaults to TRUE in the DTO — don't set explicitly

            when(employeeRepository.findByEmployeeId("ND12345")).thenReturn(Optional.of(sampleEmployee()));
            when(userRepository.existsByEmployeeId(42L)).thenReturn(false);
            when(userRepository.existsByUsername("bob")).thenReturn(false);
            when(passwordEncoder.encode("Str0ngPass!")).thenReturn("$hashed");
            when(systemConfig.getOrDefault(SystemConfigService.Keys.AUTH_DEFAULT_ROLE, "ROLE_EMPLOYEE"))
                    .thenReturn("ROLE_EMPLOYEE");
            when(roleRepository.findByName(RoleType.ROLE_EMPLOYEE))
                    .thenReturn(Optional.of(role(RoleType.ROLE_EMPLOYEE)));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            service.adminCreateUser(req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordChangedAt()).isNull();
        }
    }

}
