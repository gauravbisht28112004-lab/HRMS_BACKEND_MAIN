package com.financebuddha.finbud.hrms.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

/**
 * Admin-only payload for provisioning a user account on behalf of an
 * already-created Employee record. Used by
 * {@code POST /api/auth/admin/create-user}.
 * <p>
 * Unlike the self-service {@link RegisterRequest}, this request:
 * <ul>
 *     <li>can assign any role (ADMIN / HR / MANAGER / EMPLOYEE);</li>
 *     <li>does not require a password — when omitted, the service uses
 *         {@code auth.default_password} from system_config (Finbud seed:
 *         {@code Welcome@123});</li>
 *     <li>allows {@code email} to be omitted — loginUsername is then
 *         derived from the employeeId.</li>
 * </ul>
 * Security: this endpoint MUST be guarded by {@code hasRole('ADMIN')} —
 * see C-1 in the hardening plan.
 */
@Data
public class AdminCreateUserRequest {

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Email(message = "Email should be valid")
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /** Optional — any role the caller (ADMIN) wants to grant. Names are
     *  normalised to the ROLE_XXX form by {@code AuthService}. */
    private Set<String> roles;

    /** Force a password reset on first login. Defaults to true. */
    private Boolean mustChangePassword = Boolean.TRUE;
}
