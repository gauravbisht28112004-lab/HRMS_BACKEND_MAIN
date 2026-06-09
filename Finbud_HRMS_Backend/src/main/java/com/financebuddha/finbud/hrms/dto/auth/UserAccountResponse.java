package com.financebuddha.finbud.hrms.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Read-model for a user account as it appears on the admin / HR role-editor
 * panel. Never leaks the password hash. The {@code mustChangePassword} flag
 * is derived from whether {@code passwordChangedAt} is null (first-login
 * rotation contract).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAccountResponse {

    /** Database id of the user row (used as a stable path variable). */
    private Long userId;

    /** Login username. */
    private String username;

    /** Employee this user account is linked to. */
    private Long employeeRowId;
    private String employeeId;
    private String fullName;
    private String email;

    /** Whether the account can currently authenticate. */
    private Boolean isActive;

    /** Role names with the {@code ROLE_} prefix (e.g. ROLE_ADMIN, ROLE_EMPLOYEE). */
    private Set<String> roles;

    /** Populated from {@code user.last_login_at}. */
    private LocalDateTime lastLoginAt;

    /** Populated from {@code user.password_changed_at}. Null ⇒ must change on next login. */
    private LocalDateTime passwordChangedAt;

    /** Convenience flag derived from {@link #passwordChangedAt}. */
    private Boolean mustChangePassword;

    /** Whether the account is currently locked out (user.locked_until > now). */
    private Boolean locked;

    private LocalDateTime lockedUntil;

    private Integer failedLoginAttempts;
}
