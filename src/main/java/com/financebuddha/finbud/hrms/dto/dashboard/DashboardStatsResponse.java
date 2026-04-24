package com.financebuddha.finbud.hrms.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    // Employee stats
    private Long totalEmployees;
    private Long activeEmployees;
    private Long onLeaveEmployees;
    private Long newEmployeesThisMonth;

    // Attendance stats
    private Long presentToday;
    private Long absentToday;
    private Long lateToday;
    private Double onTimePercentage;

    // Leave stats
    private Long pendingLeaves;
    private Long approvedLeavesThisMonth;
    private Long rejectedLeavesThisMonth;

    // Payroll stats
    private BigDecimal monthlyPayroll;
    private BigDecimal totalDeductionsThisMonth;
    private Long paidPayrollsThisMonth;
    private Long pendingPayrolls;

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
