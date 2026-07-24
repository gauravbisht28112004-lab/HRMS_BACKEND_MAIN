package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.hierarchy.HierarchyDashboardResponse;
import com.financebuddha.finbud.hrms.dto.hierarchy.HierarchyReportRow;
import com.financebuddha.finbud.hrms.enums.RoleType;

import java.util.List;

/**
 * Target-flow dashboards for every level of the reporting chain
 * (Admin -> Manager -> Team Leader -> ATL -> Employee).
 */
public interface HierarchyDashboardService {

    /**
     * The signed-in user's own dashboard: their target (from the level above),
     * their whole-team disbursal, and a breakdown of their DIRECT reports only.
     *
     * @param employeeId the caller's linked employee id
     * @param tier       the caller's level in the chain (their primary role)
     */
    HierarchyDashboardResponse getMyDashboard(Long employeeId, RoleType tier, Integer year, Integer month);

    /**
     * Admin/HR flat overview of every active employee with their monthly target
     * and their OWN approved disbursal for the period — the "all employees"
     * option, separate from the "managers under me" tiered view.
     */
    List<HierarchyReportRow> getAllEmployeesOverview(Integer year, Integer month);
}
