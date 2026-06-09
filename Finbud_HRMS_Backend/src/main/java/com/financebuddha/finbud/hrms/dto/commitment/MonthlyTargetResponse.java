package com.financebuddha.finbud.hrms.dto.commitment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Per-employee per-month target with the achieved figure overlaid. Achieved
 * disbursal is computed from {@code daily_commitments} (APPROVED rows in
 * the same month) — null/zero before any commitments are recorded.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTargetResponse {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;

    private Integer year;
    private Integer month;

    private BigDecimal targetDisbursalAmount;
    private Integer targetLogins;

    /** Read-time aggregate from daily_commitments — sum of APPROVED actuals for this period. */
    private BigDecimal achievedDisbursalAmount;

    /** Convenience % = achieved / target * 100, capped at 100. 0 when target is 0. */
    private Integer achievedPercent;

    private Long setById;
    private String setByName;

    private String notes;
}
