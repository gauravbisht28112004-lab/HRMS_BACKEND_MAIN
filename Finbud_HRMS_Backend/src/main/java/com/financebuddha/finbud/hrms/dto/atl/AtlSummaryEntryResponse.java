package com.financebuddha.finbud.hrms.dto.atl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One row of the HR/Admin "all ATLs" summary — an ATL's team size and the
 * cumulative committed (target) disbursal of everyone assigned under them,
 * for the requested date window. Sorted highest-first by the caller.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtlSummaryEntryResponse {

    private Long atlId;
    private String atlCode;
    private String atlName;

    private Integer teamSize;

    private BigDecimal totalTargetDisbursalAmount;

    /** Approved actual disbursal to date for this ATL's team in the window. */
    private BigDecimal totalActualDisbursalAmount;
}
