package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.auth.LoginRequest;
import com.financebuddha.finbud.hrms.dto.auth.LoginResponse;
import com.financebuddha.finbud.hrms.dto.auth.PasswordChangeRequest;
import com.financebuddha.finbud.hrms.dto.auth.RegisterRequest;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse register(RegisterRequest request);

    void changePassword(Long userId, PasswordChangeRequest request);

    void logout(Long userId);

    LoginResponse refreshToken(String refreshToken);
}
