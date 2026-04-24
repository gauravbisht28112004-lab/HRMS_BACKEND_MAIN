package com.financebuddha.finbud.hrms.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private String username;
    private String employeeId;
    private String email;
    private String fullName;
    private List<String> roles;

    /**
     * True when the user has never rotated the default password issued at
     * import / bootstrap time. The frontend uses this to force the
     * "change password" screen before any other navigation.
     * <p>
     * Derived server-side from {@code User.passwordChangedAt == null}:
     * {@link com.financebuddha.finbud.hrms.entity.User#getPasswordChangedAt()}
     * is set only after a successful call to {@code AuthService.changePassword},
     * so a null value is the canonical "first login" signal.
     */
    private Boolean mustChangePassword;
}
