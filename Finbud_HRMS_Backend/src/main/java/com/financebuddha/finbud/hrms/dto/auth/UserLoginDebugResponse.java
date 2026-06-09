package com.financebuddha.finbud.hrms.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Admin-only diagnostic snapshot: why is {@code loginUsername} unable to
 * log in? Never leaks the password hash. Surfaces the small handful of
 * reasons Spring Security intentionally hides from end-users (user not
 * found, inactive, locked, never logged in so still on the temp password).
 *
 * <p>The companion endpoint lives on {@code AdminUserController}. Intended
 * for the HR operator to click a "diagnose login" button on the employee
 * profile screen and instantly see what needs fixing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserLoginDebugResponse {

    /** The username that was queried (echoed back so the UI can display it). */
    private String username;

    /** Does a {@code users} row with that username exist at all? */
    private Boolean exists;

    // Only populated when exists=true ---------------------------------------

    private Long userId;
    private Boolean isActive;
    /** Role names with the {@code ROLE_} prefix. */
    private Set<String> roles;

    private LocalDateTime lastLoginAt;
    private LocalDateTime passwordChangedAt;
    /** True if {@code passwordChangedAt} is null (first-login rotation still pending). */
    private Boolean neverLoggedIn;

    private Boolean locked;
    private LocalDateTime lockedUntil;
    private Integer failedLoginAttempts;

    /** Linked employee info, if any — helps the HR operator spot mismatches. */
    private String employeeCode;
    private String fullName;

    // -----------------------------------------------------------------------

    /**
     * One-line human-readable diagnosis explaining *why* login would fail
     * right now (or "All good — check password" if nothing is obviously
     * wrong). Never {@code null} so the UI always has something to show.
     */
    private String loginDiagnosis;

    /**
     * Concrete suggested admin action. UI can turn this into a button.
     * Values: {@code NONE}, {@code ACTIVATE_ACCOUNT}, {@code UNLOCK_ACCOUNT},
     * {@code RESET_PASSWORD}, {@code PROVISION_LOGIN}.
     */
    private String suggestedAction;
}
