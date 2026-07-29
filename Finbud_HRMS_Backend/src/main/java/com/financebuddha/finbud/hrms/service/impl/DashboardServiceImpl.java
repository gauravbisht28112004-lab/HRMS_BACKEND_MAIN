package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.dashboard.DashboardStatsResponse;
import com.financebuddha.finbud.hrms.entity.Department;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.enums.EmployeeStatus;
import com.financebuddha.finbud.hrms.repository.DepartmentRepository;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    public DashboardStatsResponse getDashboardStats() {
        log.info("Fetching dashboard statistics");

        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = LocalDate.of(today.getYear(), today.getMonthValue(), 1);

        // Employee stats
        Long totalEmployees = employeeRepository.count();
        Long activeEmployees = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);
        Long newEmployeesThisMonth = countNewEmployeesThisMonth(firstDayOfMonth);

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
                .newEmployeesThisMonth(newEmployeesThisMonth)
                .totalDepartments(departmentRepository.count())
                .departmentStats(departmentStats)
                .build();
    }

    private Long countNewEmployeesThisMonth(LocalDate firstDayOfMonth) {
        List<Employee> allEmployees = employeeRepository.findAll();
        return allEmployees.stream()
                .filter(emp -> emp.getDateOfJoining() != null && !emp.getDateOfJoining().isBefore(firstDayOfMonth))
                .count();
    }
}
