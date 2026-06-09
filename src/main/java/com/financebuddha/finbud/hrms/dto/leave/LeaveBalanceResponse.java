package com.financebuddha.finbud.hrms.dto.leave;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Wire shape of a per-employee per-year leave balance. Mirrors
 * {@code LeaveBalance} after the V12 Finbud-policy refactor:
 *   - casual + sick share a single pool
 *   - paid/earned has its own pool
 *   - carried-forward days are tracked separately so the UI can show
 *     "fresh + carried = effective"
 *   - WFH removed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private Integer year;

    // Casual + Sick combined pool
    private BigDecimal casualSickAllocated;
    private BigDecimal casualSickUsed;
    private BigDecimal casualSickBalance;
    private BigDecimal casualSickCarriedForward;

    // Paid / Earned leave
    private BigDecimal paidLeaveAllocated;
    private BigDecimal paidLeaveUsed;
    private BigDecimal paidLeaveBalance;
    private BigDecimal paidLeaveCarriedForward;

    // LOP is informational only — no allocation
    private BigDecimal lopDays;
}
