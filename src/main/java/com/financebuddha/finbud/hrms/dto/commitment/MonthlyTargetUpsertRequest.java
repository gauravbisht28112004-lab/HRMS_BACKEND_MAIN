package com.financebuddha.finbud.hrms.dto.commitment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Upsert payload for setting an employee's monthly target. Same (employee,
 * year, month) overwrites in place; the DB enforces uniqueness as a
 * belt-and-braces guard against races.
 */
@Data
public class MonthlyTargetUpsertRequest {

    @NotNull
    @Min(2020)
    @Max(2100)
    private Integer year;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;

    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal targetDisbursalAmount;

    @NotNull
    @Min(0)
    private Integer targetLogins;

    private String notes;
}
