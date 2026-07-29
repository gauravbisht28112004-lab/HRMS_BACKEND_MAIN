package com.financebuddha.finbud.hrms.dto.salary;

import com.financebuddha.finbud.hrms.enums.SalaryStructureType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Admin payload for creating / updating a salary structure.
 * <p>
 * The V4 CTC / NTH model is primary — {@code structureType}, {@code monthlyGrossCtc},
 * and {@code nth} are required for all new records. Legacy {@code basicSalary} /
 * {@code hra} remain optional for backward compatibility only and should NOT be
 * supplied for new Finbud employees.
 */
@Data
public class SalaryStructureRequest {

    // ------------------------------------------------------------------
    // Finbud CTC / NTH model (required)
    // ------------------------------------------------------------------

    @NotNull(message = "Structure type is required (CONTRACT / MANAGEMENT / HIGHLY_SKILLED)")
    private SalaryStructureType structureType;

    @NotNull(message = "Monthly gross CTC is required")
    @Positive(message = "Monthly gross CTC must be positive")
    private BigDecimal monthlyGrossCtc;

    @NotNull(message = "NTH (net take home) is required")
    @Positive(message = "NTH must be positive")
    private BigDecimal nth;

    @PositiveOrZero
    private BigDecimal tdsAmount;

    @PositiveOrZero
    private BigDecimal tdsRatePercent;

    @PositiveOrZero
    private BigDecimal employerPf;

    @PositiveOrZero
    private BigDecimal employeePf;

    @PositiveOrZero
    private BigDecimal employerEsi;

    @PositiveOrZero
    private BigDecimal employeeEsi;

    @PositiveOrZero
    private BigDecimal lwfAmount;

    @PositiveOrZero
    private BigDecimal incentives;

    @PositiveOrZero
    private BigDecimal otherDeductions;

    private Integer numOfMonths;

    // ------------------------------------------------------------------
    // Annual CTC — derived when omitted (monthlyGrossCtc × 12)
    // ------------------------------------------------------------------

    @PositiveOrZero
    private BigDecimal annualCtc;

    // ------------------------------------------------------------------
    // Legacy component fields — optional, retained for back-compat
    // ------------------------------------------------------------------

    @PositiveOrZero
    private BigDecimal basicSalary;

    @PositiveOrZero
    private BigDecimal hra;

    @PositiveOrZero
    private BigDecimal da;

    @PositiveOrZero
    private BigDecimal conveyanceAllowance;

    @PositiveOrZero
    private BigDecimal medicalAllowance;

    @PositiveOrZero
    private BigDecimal specialAllowance;

    @PositiveOrZero
    private BigDecimal pfEmployeePercentage;

    @PositiveOrZero
    private BigDecimal pfEmployerPercentage;

    @PositiveOrZero
    private BigDecimal esiEmployeePercentage;

    @PositiveOrZero
    private BigDecimal esiEmployerPercentage;

    @PositiveOrZero
    private BigDecimal professionalTaxAmount;

    // ------------------------------------------------------------------
    // Validity window
    // ------------------------------------------------------------------

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
    private Boolean isActive = true;
}
