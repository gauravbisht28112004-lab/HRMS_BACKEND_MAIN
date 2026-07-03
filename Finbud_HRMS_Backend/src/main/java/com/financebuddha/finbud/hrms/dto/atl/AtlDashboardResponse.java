package com.financebuddha.finbud.hrms.dto.atl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * An ATL's own dashboard: the cumulative committed (target) disbursal of
 * every employee assigned under them (direct reports via {@code manager_id}),
 * for a caller-selected date window, plus the per-employee breakdown.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtlDashboardResponse {

    private Long atlId;
    private String atlCode;
    private String atlName;

    private LocalDate startDate;
    private LocalDate endDate;

    private Integer teamSize;

    /** Sum of every team member's totalTargetDisbursalAmount — the headline number. */
    private BigDecimal totalTargetDisbursalAmount;

    private List<AtlTeamMemberCommitmentResponse> teamMembers;
}
