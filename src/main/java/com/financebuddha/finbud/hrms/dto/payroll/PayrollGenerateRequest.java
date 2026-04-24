package com.financebuddha.finbud.hrms.dto.payroll;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Unified payload for both single-employee and bulk payroll generation.
 * <p>
 * The historical shape (month + year + optional employeeId) is preserved.
 * The manual-override fields ({@code lopDays}, {@code incentivesOverride},
 * {@code adjustments}, {@code adjustmentReason}) were added so HR can run a
 * payroll without requiring perfect attendance data — a common reality for
 * the Finbud roster where attendance is still being on-boarded and the
 * master sheet drives payroll on day one.
 * <p>
 * When {@code lopDays} is present it takes precedence over the attendance
 * computation. When {@code null}, the service falls back to deriving LOP
 * from Attendance records.
 */
@Data
public class PayrollGenerateRequest {

    @NotNull(message = "Month is required")
    @Min(1)
    @Max(12)
    private Integer month;

    @NotNull(message = "Year is required")
    @Min(2000)
    private Integer year;

    /** If null, generate for all ACTIVE employees. */
    private Long employeeId;

    // ------------------------------------------------------------------
    // Per-run manual overrides (all optional)
    // ------------------------------------------------------------------

    /** Manual LOP (Loss-of-Pay) days. When non-null, bypasses attendance. */
    @PositiveOrZero
    private BigDecimal lopDays;

    /** One-off incentive paid this run, overrides the structure's standing incentives. */
    @PositiveOrZero
    private BigDecimal incentivesOverride;

    /** Free-form positive or negative adjustment applied to net pay. */
    private BigDecimal adjustments;

    /** Reason shown on the payslip when adjustments is non-zero. */
    private String adjustmentReason;
}
