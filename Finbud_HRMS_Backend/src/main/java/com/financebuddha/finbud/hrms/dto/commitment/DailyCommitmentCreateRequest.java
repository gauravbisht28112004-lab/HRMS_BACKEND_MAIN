package com.financebuddha.finbud.hrms.dto.commitment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Employee creates the day's commitment with their targets. Actuals are
 * filled separately via the update endpoint at end of day. Status starts
 * at DRAFT — the row stays editable until the employee Submits.
 */
@Data
public class DailyCommitmentCreateRequest {

    @NotNull
    private LocalDate workDate;

    @NotNull
    @Min(0)
    private Integer targetCalls;

    @NotNull
    @Min(0)
    private Integer targetOtps;

    @NotNull
    @Min(0)
    private Integer targetInterestedCustomers;

    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal targetDisbursalAmount;

    private String notes;
}
