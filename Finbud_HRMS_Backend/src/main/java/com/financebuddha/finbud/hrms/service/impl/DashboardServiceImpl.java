package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.dashboard.DashboardStatsResponse;
import com.financebuddha.finbud.hrms.entity.Department;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.LeaveRequest;
import com.financebuddha.finbud.hrms.entity.Payroll;
import com.financebuddha.finbud.hrms.enums.EmployeeStatus;
import com.financebuddha.finbud.hrms.enums.LeaveStatus;
import com.financebuddha.finbud.hrms.enums.PayrollStatus;
import com.financebuddha.finbud.hrms.repository.DepartmentRepository;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.LeaveRequestRepository;
import com.financebuddha.finbud.hrms.repository.PayrollRepository;
import com.financebuddha.finbud.hrms.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollRepository payrollRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    public DashboardStatsResponse getDashboardStats() {
        log.info("Fetching dashboard statistics");

        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int currentYear = today.getYear();
        LocalDate firstDayOfMonth = LocalDate.of(currentYear, currentMonth, 1);

        // Employee stats
        Long totalEmployees = employeeRepository.count();
        Long activeEmployees = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        Long onLeaveEmployees = countEmployeesOnLeaveToday(today);
        Long newEmployeesThisMonth = countNewEmployeesThisMonth(firstDayOfMonth);

        // Leave stats
        Long pendingLeaves = leaveRequestRepository.findByStatus(LeaveStatus.PENDING, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        Long approvedLeavesThisMonth = countLeavesByStatusAndMonth(LeaveStatus.APPROVED, currentMonth, currentYear);
        Long rejectedLeavesThisMonth = countLeavesByStatusAndMonth(LeaveStatus.REJECTED, currentMonth, currentYear);

        // Payroll stats
        Double monthlyPayrollSum = payrollRepository.sumNetPayByMonthAndYear(currentMonth, currentYear);
        BigDecimal monthlyPayroll = monthlyPayrollSum != null ? BigDecimal.valueOf(monthlyPayrollSum) : BigDecimal.ZERO;
        Double totalDeductionsSum = payrollRepository.sumTotalDeductionsByMonthAndYear(currentMonth, currentYear);
        BigDecimal totalDeductionsThisMonth = totalDeductionsSum != null ? BigDecimal.valueOf(totalDeductionsSum) : BigDecimal.ZERO;
        Long paidPayrollsThisMonth = (long) payrollRepository.findByMonthAndYearAndStatus(currentMonth, currentYear, PayrollStatus.PAID).size();
        Long pendingPayrolls = (long) payrollRepository.findByStatus(PayrollStatus.DRAFT, org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        // Department stats
        List<Department> departments = departmentRepository.findAll();
        List<DashboardStatsResponse.DepartmentStat> departmentStats = departments.stream()
                .map(dept -> {
                    Long empCount = departmentRepository.countEmployeesByDepartment(dept.getId());
                    return DashboardStatsResponse.DepartmentStat.builder()
                            .departmentId(dept.getId())
                            .departmentName(dept.getName())
                            .employeeCount(empCount)
                            .build();
                })
                .collect(Collectors.toList());

        return DashboardStatsResponse.builder()
                .totalEmployees(totalEmployees)
                .activeEmployees(activeEmployees)
                .onLeaveEmployees(onLeaveEmployees)
                .newEmployeesThisMonth(newEmployeesThisMonth)
                .pendingLeaves(pendingLeaves)
                .approvedLeavesThisMonth(approvedLeavesThisMonth)
                .rejectedLeavesThisMonth(rejectedLeavesThisMonth)
                .monthlyPayroll(monthlyPayroll)
                .totalDeductionsThisMonth(totalDeductionsThisMonth)
                .paidPayrollsThisMonth(paidPayrollsThisMonth)
                .pendingPayrolls(pendingPayrolls)
                .totalDepartments(departmentRepository.count())
                .departmentStats(departmentStats)
                .build();
    }

    private Long countEmployeesOnLeaveToday(LocalDate date) {
        List<LeaveRequest> leavesOnDate = leaveRequestRepository.findApprovedLeavesByEmployeeAndDateRange(0L, date, date);
        // Get all approved leaves that cover today
        List<LeaveRequest> allApprovedLeaves = leaveRequestRepository.findByStatus(LeaveStatus.APPROVED, org.springframework.data.domain.Pageable.unpaged()).getContent();
        return allApprovedLeaves.stream()
                .filter(leave -> !leave.getStartDate().isAfter(date) && !leave.getEndDate().isBefore(date))
                .map(LeaveRequest::getEmployee)
                .distinct()
                .count();
    }

    private Long countNewEmployeesThisMonth(LocalDate firstDayOfMonth) {
        List<Employee> allEmployees = employeeRepository.findAll();
        return allEmployees.stream()
                .filter(emp -> !emp.getDateOfJoining().isBefore(firstDayOfMonth))
                .count();
    }

    private Long countLeavesByStatusAndMonth(LeaveStatus status, int month, int year) {
        List<LeaveRequest> allLeaves = leaveRequestRepository.findByStatus(status, org.springframework.data.domain.Pageable.unpaged()).getContent();
        return allLeaves.stream()
                .filter(leave -> leave.getStartDate().getMonthValue() == month && leave.getStartDate().getYear() == year)
                .count();
    }
}
