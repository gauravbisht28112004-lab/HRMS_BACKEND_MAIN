package com.financebuddha.finbud.hrms.dto.hierarchy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * A single level's target-flow dashboard, uniform across the whole chain:
 *
 * <pre>
 *   Admin  ->  Manager  ->  Team Leader  ->  ATL  ->  Employee
 * </pre>
 *
 * <p>Every dashboard answers the same two questions the business cares about:
 * <ol>
 *   <li><b>What is my target?</b> — {@code myTargetDisbursalAmount}, the
 *       monthly target handed down by the level above (Admin sets the
 *       Manager's, the Manager sets each Team Leader's, and so on).</li>
 *   <li><b>How much has my team disbursed so far?</b> —
 *       {@code teamDisbursedToDate}, the APPROVED actual disbursal summed
 *       over the owner's ENTIRE downstream chain for the month.</li>
 * </ol>
 *
 * <p>{@code reports} lists only the owner's DIRECT reports; each carries that
 * report's own whole-team rollup, so no level ever sees data belonging to a
 * level more than one step below it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HierarchyDashboardResponse {

    private Long employeeId;
    private String employeeCode;
    private String employeeName;

    /** The owner's level label: "Admin" / "Manager" / "Team Leader" / "ATL" / "Employee". */
    private String roleLabel;

    private Integer year;
    private Integer month;

    /** Target assigned to the owner by the level above (0 if none set yet). */
    private BigDecimal myTargetDisbursalAmount;

    /** Whole downstream team's APPROVED actual disbursal for the period. */
    private BigDecimal teamDisbursedToDate;

    /** teamDisbursedToDate / myTargetDisbursalAmount, capped at 100 (0 if no target). */
    private Integer teamAchievedPercent;

    /** Sum of the targets the owner has assigned across their direct reports. */
    private BigDecimal allocatedToReports;

    /**
     * myTargetDisbursalAmount − allocatedToReports. Positive = still to assign;
     * negative = over-allocated. Drives the soft-warning on the assign screen
     * (assignment is never blocked, only flagged).
     */
    private BigDecimal unallocatedRemaining;

    /** Label for the {@code reports} rows: "Manager" / "Team Leader" / "ATL" / "Employee". */
    private String reportsRoleLabel;

    private List<HierarchyReportRow> reports;
}
