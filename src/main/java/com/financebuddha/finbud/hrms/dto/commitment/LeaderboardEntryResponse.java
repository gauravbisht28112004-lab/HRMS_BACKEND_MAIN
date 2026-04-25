package com.financebuddha.finbud.hrms.dto.commitment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One row of the monthly disbursal leaderboard. Q3 of the feature triage.
 *
 * <p>Aggregate is over APPROVED daily commitments only — unverified data
 * doesn't pollute the rankings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryResponse {
    /** 1-indexed rank in the returned list (handy for the UI to render the badge). */
    private Integer rank;

    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String department;

    /** Sum of APPROVED daily disbursal in the requested period. */
    private BigDecimal totalDisbursalAmount;
}
