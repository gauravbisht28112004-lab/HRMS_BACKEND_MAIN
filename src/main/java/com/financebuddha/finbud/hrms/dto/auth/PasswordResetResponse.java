package com.financebuddha.finbud.hrms.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload for {@code PATCH /api/admin/users/{userId}/password/reset}.
 * The plaintext password is returned once for the admin UI to display in a
 * copy-once modal. After the user's next login the {@code mustChangePassword}
 * contract forces a rotation, so this plaintext is usable for exactly one
 * login session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PasswordResetResponse {

    private Long userId;
    private String username;

    /** The plaintext password now stored (hashed) on the account. One-time leak. */
    private String temporaryPassword;

    /** Always {@code true} after reset — next login forces a rotation. */
    private Boolean mustChangePassword;
}
