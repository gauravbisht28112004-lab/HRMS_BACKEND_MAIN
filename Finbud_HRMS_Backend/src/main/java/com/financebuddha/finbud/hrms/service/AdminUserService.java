package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.auth.BulkPasswordResetResponse;
import com.financebuddha.finbud.hrms.dto.auth.PasswordResetResponse;
import com.financebuddha.finbud.hrms.dto.auth.ProvisionMissingUsersResponse;
import com.financebuddha.finbud.hrms.dto.auth.UpdateUserRolesRequest;
import com.financebuddha.finbud.hrms.dto.auth.UpdateUserStatusRequest;
import com.financebuddha.finbud.hrms.dto.auth.UserAccountResponse;
import com.financebuddha.finbud.hrms.dto.auth.UserLoginDebugResponse;

/**
 * Admin / HR surface for managing login accounts already linked to
 * employees. The {@code POST /api/admin/users} creation flow lives on the
 * existing {@link AuthService#adminCreateUser(com.financebuddha.finbud.hrms.dto.auth.AdminCreateUserRequest)}
 * — this service exists to keep the read + update flows cohesive.
 * <p>
 * <b>Authorisation contract</b> enforced inside the implementation:
 * <ul>
 *     <li>All methods require the caller to hold at least one of
 *         {@code ROLE_ADMIN} / {@code ROLE_HR}; matched at the request
 *         layer (SecurityConfig + {@code @PreAuthorize}).</li>
 *     <li>When the caller is <i>not</i> an Admin, {@link #updateRoles} may
 *         only grant or revoke {@code ROLE_EMPLOYEE} / {@code ROLE_MANAGER}.
 *         Attempting to grant/revoke {@code ROLE_ADMIN} or {@code ROLE_HR}
 *         from an HR session throws {@code ForbiddenException}.</li>
 * </ul>
 */
public interface AdminUserService {

    /** Fetch a user account by its DB id. 404 when missing. */
    UserAccountResponse getByUserId(Long userId);

    /**
     * Fetch the user account linked to an employee's string code
     * (e.g. {@code FBD260005}, {@code ND33454}). 404 when either the
     * employee or its linked user is missing.
     */
    UserAccountResponse getByEmployeeCode(String employeeCode);

    /**
     * Replace the user's role set with the requested set. HR callers are
     * restricted to granting/revoking Employee + Manager roles only.
     */
    UserAccountResponse updateRoles(Long userId, UpdateUserRolesRequest request);

    /** Activate or deactivate the account. */
    UserAccountResponse updateStatus(Long userId, UpdateUserStatusRequest request);

    /**
     * Reset the account's password back to {@code auth.default_password}
     * and clear {@code passwordChangedAt} so the next login forces a
     * rotation. Also unlocks the account and zeroes the failed-attempt
     * counter so the user can actually log in with the new credentials.
     */
    PasswordResetResponse resetPassword(Long userId);

    /**
     * Diagnostic lookup: given a username that can't log in, figure out
     * *why*. Returns a clear reason (account missing / inactive / locked /
     * never-rotated-password) plus a suggested admin action. Never leaks
     * the password hash. No-op safe if the username doesn't exist.
     */
    UserLoginDebugResponse debugLogin(String username);

    /**
     * Bulk-reset every user who has never logged in (passwordChangedAt IS
     * NULL) back to the system default password. Also clears any failed
     * login attempts and unlocks the account. Used to recover from
     * provisioning runs where the original temp passwords were lost.
     *
     * <p>Safe by design — only touches users who never customised their
     * password. Returns the number of users reset and the default password
     * itself so the admin can broadcast it to the affected employees.
     *
     * <p>Admin only. HR cannot bulk-reset (they could accidentally include
     * Admin/HR accounts which the per-user reset would normally protect).
     */
    BulkPasswordResetResponse bulkResetUntouchedAccounts();

    /**
     * Finds every active employee that has no User row and provisions a login
     * account for each one, exactly as the Excel import flow does:
     * <ul>
     *     <li>Username = {@code employee.loginUsername} if set, otherwise
     *         {@code employeeId.toLowerCase()} (e.g. {@code nd33447})</li>
     *     <li>Password = {@code system_config.auth.default_password} (default: {@code finbud@123})</li>
     *     <li>Role = {@code ROLE_EMPLOYEE}</li>
     *     <li>{@code passwordChangedAt} left null → forces rotation on first login</li>
     * </ul>
     * Safe to call repeatedly — employees that already have a User account are skipped.
     * Admin only.
     */
    ProvisionMissingUsersResponse provisionMissingUsers();
}
