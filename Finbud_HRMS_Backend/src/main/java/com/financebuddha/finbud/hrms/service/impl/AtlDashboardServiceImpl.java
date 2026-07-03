package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.atl.AtlDashboardResponse;
import com.financebuddha.finbud.hrms.dto.atl.AtlSummaryEntryResponse;
import com.financebuddha.finbud.hrms.dto.atl.AtlTeamMemberCommitmentResponse;
import com.financebuddha.finbud.hrms.entity.DailyCommitment;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.enums.RoleType;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.repository.DailyCommitmentRepository;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.UserRepository;
import com.financebuddha.finbud.hrms.service.AtlDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ATL dashboard — cumulative <em>committed</em> (target, any status)
 * disbursal of the employees assigned under an ATL, and the HR/Admin
 * rollup across every ATL.
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
    private final DailyCommitmentRepository dailyCommitmentRepository;

    @Override
    @Transactional(readOnly = true)
    public AtlDashboardResponse getTeamDashboard(Long atlId, LocalDate startDate, LocalDate endDate) {
        Employee atl = employeeRepository.findById(atlId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", atlId));

        List<DailyCommitment> rows = dailyCommitmentRepository
                .findByManagerIdAndWorkDateBetween(atlId, startDate, endDate);

        Map<Long, Employee> employeesById = new LinkedHashMap<>();
        Map<Long, BigDecimal> totalsByEmployee = new LinkedHashMap<>();
        for (DailyCommitment c : rows) {
            Employee e = c.getEmployee();
            employeesById.putIfAbsent(e.getId(), e);
            totalsByEmployee.merge(e.getId(),
                    c.getTargetDisbursalAmount() == null ? BigDecimal.ZERO : c.getTargetDisbursalAmount(),
                    BigDecimal::add);
        }

        // Include direct reports with zero activity in the window too — the
        // ATL should see their whole roster, not just employees who logged
        // a commitment.
        for (Employee e : employeeRepository.findActiveSubordinates(atlId)) {
            employeesById.putIfAbsent(e.getId(), e);
            totalsByEmployee.putIfAbsent(e.getId(), BigDecimal.ZERO);
        }

        List<AtlTeamMemberCommitmentResponse> members = employeesById.values().stream()
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

        List<Long> atlIds = atls.stream().map(Employee::getId).toList();
        Map<Long, BigDecimal> totalsByAtl = dailyCommitmentRepository
                .aggregateTargetDisbursalByManagerIds(atlIds, startDate, endDate).stream()
                .collect(java.util.stream.Collectors.toMap(
                        DailyCommitmentRepository.ManagerAggregateRow::getManagerId,
                        DailyCommitmentRepository.ManagerAggregateRow::getTotal));

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
