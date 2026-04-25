package com.financebuddha.finbud.hrms.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response from the bulk-reset endpoint that resets every never-logged-in
 * user back to the system default password. The {@code defaultPassword}
 * field carries the plain-text default so the admin can broadcast it once
 * — it is the same value stored in {@code system_config.auth.default_password}
 * and is therefore not a secret.
 *
 * <p>The {@code resetUsernames} list lets the admin generate a CSV/Excel
 * to email out, or just verify which accounts were actually touched.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPasswordResetResponse {

    /** Number of users whose password was just reset. */
    private Integer resetCount;

    /** Number of users skipped because they had already logged in. */
    private Integer skippedCount;

    /** Default password they were reset to (broadcast this to employees). */
    private String defaultPassword;

    /** Usernames that were reset. Useful for HR to generate a notification list. */
    private List<String> resetUsernames;
}
