package com.financebuddha.finbud.hrms.dto.atl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One row in an ATL's team-commitment breakdown — a single direct-report
 * employee's total committed (target) disbursal within the requested date
 * window.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtlTeamMemberCommitmentResponse {

    private Long employeeId;
    private String employeeCode;
    private String employeeName;

    /** Sum of targetDisbursalAmount for this employee across the date window, any status. */
    private BigDecimal totalTargetDisbursalAmount;
}
