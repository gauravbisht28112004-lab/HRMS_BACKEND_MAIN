package com.financebuddha.finbud.hrms.dto.hierarchy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One direct-report row on a hierarchy dashboard.
 *
 * <p>Represents a single person one level below the dashboard owner — e.g. a
 * Team Leader row on a Manager's dashboard, or an employee row on an ATL's
 * dashboard. The disbursal figure is that report's WHOLE-TEAM rollup (their
 * own subtree), so a Manager sees each Team Leader's entire chain collapsed
 * into one number — never the individual people beneath that Team Leader.
 * This is what enforces the "no overlapping data" rule between levels.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HierarchyReportRow {

    private Long employeeId;
    private String employeeCode;
    private String employeeName;

    /** Monthly target this report was assigned by the dashboard owner (0 if none set). */
    private BigDecimal assignedTargetDisbursalAmount;

    /** This report's whole-team approved actual disbursal for the period. */
    private BigDecimal teamDisbursedToDate;

    /** Number of people in this report's downstream team (subtree size). */
    private Integer teamSize;

    /** teamDisbursedToDate / assignedTargetDisbursalAmount, capped at 100 (0 if no target). */
    private Integer achievedPercent;
}
