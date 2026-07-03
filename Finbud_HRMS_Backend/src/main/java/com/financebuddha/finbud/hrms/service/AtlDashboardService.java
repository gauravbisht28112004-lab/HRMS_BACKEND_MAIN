package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.atl.AtlDashboardResponse;
import com.financebuddha.finbud.hrms.dto.atl.AtlSummaryEntryResponse;

import java.time.LocalDate;
import java.util.List;

public interface AtlDashboardService {

    /**
     * One ATL's own dashboard: cumulative committed (target) disbursal of
     * every employee assigned under them, for the given date window, plus
     * a per-employee breakdown. Includes direct reports with zero activity
     * in the window so the ATL sees their whole roster, not just employees
     * who logged something.
     */
    AtlDashboardResponse getTeamDashboard(Long atlId, LocalDate startDate, LocalDate endDate);

    /**
     * HR/Admin view: one row per employee holding ROLE_ATL, each with their
     * team size and cumulative committed disbursal for the date window.
     * Sorted highest-committed-first.
     */
    List<AtlSummaryEntryResponse> getAtlSummary(LocalDate startDate, LocalDate endDate);
}
