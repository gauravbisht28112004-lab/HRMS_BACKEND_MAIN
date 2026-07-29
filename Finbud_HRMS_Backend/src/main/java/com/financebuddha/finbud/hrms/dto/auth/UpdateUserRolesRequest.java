package com.financebuddha.finbud.hrms.dto.auth;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

/**
 * Payload for {@code PATCH /api/admin/users/{userId}/roles}. The service
 * normalises role names to the {@code ROLE_XXX} form and rejects any role
 * the caller doesn't have authority to grant (see
 * {@code AdminUserService#updateRoles}).
 * <p>
 * Semantics:
 * <ul>
 *     <li>The set is authoritative — the user's resulting role set is
 *         exactly {@link #roles} (additions and removals as needed).</li>
 *     <li>At least one role is required; an account with zero roles would
 *         be unable to do anything useful.</li>
 *     <li>HR callers may only grant/revoke {@code ROLE_EMPLOYEE},
 *         {@code ROLE_MANAGER}, and {@code ROLE_ATL}. Attempting to grant
 *         {@code ROLE_ADMIN} or {@code ROLE_HR} from an HR session returns
 *         403.</li>
 * </ul>
 */
@Data
public class UpdateUserRolesRequest {

    @NotEmpty(message = "At least one role must be provided")
    private Set<String> roles;
}
