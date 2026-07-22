package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.atl.AtlDashboardResponse;
import com.financebuddha.finbud.hrms.dto.atl.AtlSummaryEntryResponse;
import com.financebuddha.finbud.hrms.dto.atl.AtlTeamMemberCommitmentResponse;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.enums.RoleType;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.MonthlyTargetRepository;
import com.financebuddha.finbud.hrms.repository.UserRepository;
import com.financebuddha.finbud.hrms.service.AtlDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ATL dashboard — cumulative <em>assigned monthly target</em> disbursal of
 * the employees assigned under an ATL (summed over the month(s) a date range
 * spans), and the HR/Admin rollup across every ATL.
 *
 * <p>"Assigned under" reuses the existing {@code Employee.manager}
 * self-reference — the same mechanic HR already uses to build MANAGER
 * teams (via {@code PUT /api/employees/{id}} with a {@code managerId}).
 * An ATL is just an employee holding {@link RoleType#ROLE_ATL} with
 * subordinates pointed at them; no new hierarchy table.
 */
@Service
@RequiredArgsConstructor
public class AtlDashboardServiceImpl implements AtlDashboardService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final MonthlyTargetRepository monthlyTargetRepository;

    @Override
    @Transactional(readOnly = true)
    public AtlDashboardResponse getTeamDashboard(Long atlId, LocalDate startDate, LocalDate endDate) {
        Employee atl = employeeRepository.findById(atlId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", atlId));

        int startYear = startDate.getYear();
        int startMonth = startDate.getMonthValue();
        int endYear = endDate.getYear();
        int endMonth = endDate.getMonthValue();

        // Per-employee sum of the ASSIGNED monthly targets over the month(s)
        // the date range spans — the same figures set on the "Assign Targets"
        // screen (not the employees' self-entered daily commitments).
        Map<Long, BigDecimal> totalsByEmployee = monthlyTargetRepository
                .aggregateTargetByEmployeeForManager(atlId, startYear, startMonth, endYear, endMonth).stream()
                .collect(java.util.stream.Collectors.toMap(
                        MonthlyTargetRepository.EmployeeTargetAggregateRow::getEmployeeId,
                        MonthlyTargetRepository.EmployeeTargetAggregateRow::getTotal));

        // Every active direct report, including those without a target yet (0).
        List<AtlTeamMemberCommitmentResponse> members = employeeRepository.findActiveSubordinates(atlId).stream()
                .map(e -> AtlTeamMemberCommitmentResponse.builder()
                        .employeeId(e.getId())
                        .employeeCode(e.getEmployeeId())
                        .employeeName(e.getFullName())
                        .totalTargetDisbursalAmount(totalsByEmployee.getOrDefault(e.getId(), BigDecimal.ZERO))
                        .build())
                .sorted(Comparator.comparing(AtlTeamMemberCommitmentResponse::getTotalTargetDisbursalAmount).reversed())
                .toList();

        BigDecimal cumulative = members.stream()
                .map(AtlTeamMemberCommitmentResponse::getTotalTargetDisbursalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AtlDashboardResponse.builder()
                .atlId(atl.getId())
                .atlCode(atl.getEmployeeId())
                .atlName(atl.getFullName())
                .startDate(startDate)
                .endDate(endDate)
                .teamSize(members.size())
                .totalTargetDisbursalAmount(cumulative)
                .teamMembers(members)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AtlSummaryEntryResponse> getAtlSummary(LocalDate startDate, LocalDate endDate) {
        List<Employee> atls = userRepository.findActiveUsersByRoleName(RoleType.ROLE_ATL).stream()
                .map(User::getEmployee)
                .filter(Objects::nonNull)
                .toList();

        if (atls.isEmpty()) {
            return List.of();
        }

        int startYear = startDate.getYear();
        int startMonth = startDate.getMonthValue();
        int endYear = endDate.getYear();
        int endMonth = endDate.getMonthValue();

        List<Long> atlIds = atls.stream().map(Employee::getId).toList();
        Map<Long, BigDecimal> totalsByAtl = monthlyTargetRepository
                .aggregateTargetByManagerIds(atlIds, startYear, startMonth, endYear, endMonth).stream()
                .collect(java.util.stream.Collectors.toMap(
                        MonthlyTargetRepository.ManagerTargetAggregateRow::getManagerId,
                        MonthlyTargetRepository.ManagerTargetAggregateRow::getTotal));

        return atls.stream()
                .map(atl -> AtlSummaryEntryResponse.builder()
                        .atlId(atl.getId())
                        .atlCode(atl.getEmployeeId())
                        .atlName(atl.getFullName())
                        .teamSize(employeeRepository.findActiveSubordinates(atl.getId()).size())
                        .totalTargetDisbursalAmount(totalsByAtl.getOrDefault(atl.getId(), BigDecimal.ZERO))
                        .build())
                .sorted(Comparator.comparing(AtlSummaryEntryResponse::getTotalTargetDisbursalAmount).reversed())
                .toList();
    }
}
