package com.financebuddha.finbud.hrms.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    // Employee stats
    private Long totalEmployees;
    private Long activeEmployees;
    private Long newEmployeesThisMonth;

    // Department stats
    private Long totalDepartments;
    private List<DepartmentStat> departmentStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentStat {
        private Long departmentId;
        private String departmentName;
        private Long employeeCount;
    }
}
