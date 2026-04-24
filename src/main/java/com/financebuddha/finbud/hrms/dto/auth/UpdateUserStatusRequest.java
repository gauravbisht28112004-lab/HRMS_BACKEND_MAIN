package com.financebuddha.finbud.hrms.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Payload for {@code PATCH /api/admin/users/{userId}/status} — activate or
 * deactivate a login account without touching the Employee row.
 */
@Data
public class UpdateUserStatusRequest {

    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
