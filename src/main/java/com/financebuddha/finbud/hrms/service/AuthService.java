package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.auth.AdminCreateUserRequest;
import com.financebuddha.finbud.hrms.dto.auth.LoginRequest;
import com.financebuddha.finbud.hrms.dto.auth.LoginResponse;
import com.financebuddha.finbud.hrms.dto.auth.PasswordChangeRequest;
import com.financebuddha.finbud.hrms.dto.auth.RegisterRequest;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse register(RegisterRequest request);

    /**
     * Admin-only: provision a User for an existing Employee, optionally
     * granting non-default roles (ADMIN / HR / MANAGER). Used by
     * {@code POST /api/admin/users}. Returns a {@link LoginResponse} for
     * convenience but the access/refresh tokens are NOT meant to be used —
     * the new account must log in to generate its own session.
     * <p>
     * If {@code request.getPassword()} is null/blank, the configured
     * {@code auth.default_password} is used (Finbud seed: {@code Welcome@123}).
     * If {@code request.getRoles()} is null/empty, ROLE_EMPLOYEE is granted.
     * Unknown role names are ignored with a warning.
     */
    LoginResponse adminCreateUser(AdminCreateUserRequest request);

    void changePassword(Long userId, PasswordChangeRequest request);

    void logout(Long userId);

    LoginResponse refreshToken(String refreshToken);
}
