package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.auth.AdminCreateUserRequest;
import com.financebuddha.finbud.hrms.dto.auth.BulkPasswordResetResponse;
import com.financebuddha.finbud.hrms.dto.auth.LoginResponse;
import com.financebuddha.finbud.hrms.dto.auth.PasswordResetResponse;
import com.financebuddha.finbud.hrms.dto.auth.UpdateUserRolesRequest;
import com.financebuddha.finbud.hrms.dto.auth.UpdateUserStatusRequest;
import com.financebuddha.finbud.hrms.dto.auth.UserAccountResponse;
import com.financebuddha.finbud.hrms.dto.auth.UserLoginDebugResponse;
import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.service.AdminUserService;
import com.financebuddha.finbud.hrms.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin / HR surface for managing login accounts.
 * <p>
 * Routing contract:
 * <ul>
 *     <li>{@code POST /api/admin/users} — Admin-only. Provisions a User for
 *         an existing Employee with arbitrary roles.</li>
 *     <li>{@code GET}/{@code PATCH} under {@code /api/admin/users} — Admin
 *         and HR, with server-side guards that stop HR from
 *         granting/revoking privileged roles (see
 *         {@link com.financebuddha.finbud.hrms.service.impl.AdminUserServiceImpl}).</li>
 * </ul>
 * <p>
 * SecurityConfig declares {@code /api/admin/users/**} → {@code hasAnyRole('ADMIN','HR')}
 * <i>before</i> the blanket {@code /api/admin/**} → ADMIN matcher, so HR
 * sessions reach this controller. The per-method {@code @PreAuthorize}
 * annotations below are defense-in-depth and additionally restrict the
 * create endpoint to Admin only.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin: Users", description = "Admin / HR user account management")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AuthService authService;
    private final AdminUserService adminUserService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Provision a user account",
               description = "Admin-only: create a User for an existing Employee with optional roles. "
                           + "Password defaults to system_config.auth.default_password if omitted.")
    public ResponseEntity<ApiResponse<LoginResponse>> createUser(
            @Valid @RequestBody AdminCreateUserRequest request) {
        LoginResponse response = authService.adminCreateUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User provisioned for employee " + request.getEmployeeId(), response));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Get a user account by id")
    public ResponseEntity<ApiResponse<UserAccountResponse>> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getByUserId(userId)));
    }

    @GetMapping("/by-employee/{employeeCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Get the user account linked to an employee code",
               description = "Looks up the user by employee code (e.g. ND33454, FBD260005). "
                           + "Returns 404 if the employee has no login provisioned yet.")
    public ResponseEntity<ApiResponse<UserAccountResponse>> getUserByEmployeeCode(
            @PathVariable String employeeCode) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getByEmployeeCode(employeeCode)));
    }

    @PatchMapping("/{userId}/roles")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Update a user's roles",
               description = "Replaces the user's role set. HR callers may only grant or revoke "
                           + "ROLE_EMPLOYEE / ROLE_MANAGER; attempting to touch ROLE_ADMIN or "
                           + "ROLE_HR from an HR session returns 403.")
    public ResponseEntity<ApiResponse<UserAccountResponse>> updateRoles(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRolesRequest request) {
        UserAccountResponse response = adminUserService.updateRoles(userId, request);
        return ResponseEntity.ok(ApiResponse.success("User roles updated", response));
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Activate or deactivate an account",
               description = "HR may not toggle Admin or HR accounts — only an Admin can.")
    public ResponseEntity<ApiResponse<UserAccountResponse>> updateStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        UserAccountResponse response = adminUserService.updateStatus(userId, request);
        String verb = Boolean.TRUE.equals(request.getIsActive()) ? "activated" : "deactivated";
        return ResponseEntity.ok(ApiResponse.success("User " + verb, response));
    }

    @PatchMapping("/{userId}/password/reset")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Reset a user's password to the default",
               description = "Resets to system_config.auth.default_password and forces a rotation on next login. "
                           + "HR cannot reset the password of an Admin or HR account.")
    public ResponseEntity<ApiResponse<PasswordResetResponse>> resetPassword(@PathVariable Long userId) {
        PasswordResetResponse response = adminUserService.resetPassword(userId);
        return ResponseEntity.ok(ApiResponse.success(
                "Password reset to default — user must change on next login", response));
    }

    @GetMapping("/debug/{username}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Diagnose a login failure",
               description = "Given a username that can't log in, figure out why: not provisioned, inactive, "
                           + "locked, never-rotated password, or simply the wrong password. Returns a plain-English "
                           + "diagnosis and a suggested action for the admin UI to surface as a button. "
                           + "Never returns the password hash.")
    public ResponseEntity<ApiResponse<UserLoginDebugResponse>> debugLogin(@PathVariable String username) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.debugLogin(username)));
    }

    @PostMapping("/bulk-reset-passwords")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk-reset every never-logged-in user to the default password",
               description = "One-shot recovery from a provisioning run where the original temp passwords were "
                           + "lost. Resets every user with passwordChangedAt IS NULL (i.e. never-rotated) back "
                           + "to system_config.auth.default_password. Skips Admin and HR seed accounts. Returns "
                           + "the default password and the list of usernames that were reset so HR can broadcast "
                           + "credentials.")
    public ResponseEntity<ApiResponse<BulkPasswordResetResponse>> bulkResetPasswords() {
        BulkPasswordResetResponse response = adminUserService.bulkResetUntouchedAccounts();
        return ResponseEntity.ok(ApiResponse.success(
                "Reset " + response.getResetCount() + " account(s) to default", response));
    }
}
