package com.financebuddha.finbud.hrms.dto.commitment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Patch payload — the employee revises targets, fills actuals, or both.
 * All fields optional; the service ignores nulls so the UI can send only
 * what changed. Only valid while status is {@code DRAFT} or
 * {@code REJECTED} — APPROVED commitments are read-only (HR override
 * required to change them, which is a different endpoint).
 */
@Data
public class DailyCommitmentUpdateRequest {

    @Min(0)
    private Integer targetCalls;

    @Min(0)
    private Integer targetOtps;

    @Min(0)
    private Integer targetInterestedCustomers;

    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal targetDisbursalAmount;

    @Min(0)
    private Integer actualCalls;

    @Min(0)
    private Integer actualOtps;

    @Min(0)
    private Integer actualInterestedCustomers;

    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal actualDisbursalAmount;

    private String notes;
}
