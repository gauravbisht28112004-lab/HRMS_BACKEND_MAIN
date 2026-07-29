package com.financebuddha.finbud.hrms.dto.shift;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request payload for creating or updating a shift assignment.
 *
 * <p>employeeId is carried on the path for the create / list flows, so
 * it is intentionally NOT part of the body — the service layer pulls it
 * from the URL and binds it on the entity. The body is just the shift
 * being assigned and the validity window.</p>
 *
 * <p>effectiveTo is nullable → "open-ended assignment, currently active".</p>
 */
@Data
public class ShiftAssignmentRequest {

    @NotNull(message = "Shift type id is required")
    private Long shiftTypeId;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}
