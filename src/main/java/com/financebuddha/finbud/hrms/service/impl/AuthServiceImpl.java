package com.financebuddha.finbud.hrms.service.impl;

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

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", request.getUsername()));

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

        Set<Role> roles = new HashSet<>();
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            Role employeeRole = roleRepository.findByName(RoleType.ROLE_EMPLOYEE)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "ROLE_EMPLOYEE"));
            roles.add(employeeRole);
        } else {
            request.getRoles().forEach(roleName -> {
                RoleType roleType = RoleType.valueOf(roleName);
                Role role = roleRepository.findByName(roleType)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));
                roles.add(role);
            });
        }
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
                .build();
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
                .build();
    }
}
