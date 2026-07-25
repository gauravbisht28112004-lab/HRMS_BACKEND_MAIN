package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.hierarchy.HierarchyDashboardResponse;
import com.financebuddha.finbud.hrms.dto.hierarchy.HierarchyReportRow;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.MonthlyTarget;
import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.enums.EmployeeStatus;
import com.financebuddha.finbud.hrms.enums.RoleType;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.repository.DailyCommitmentRepository;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.MonthlyTargetRepository;
import com.financebuddha.finbud.hrms.repository.UserRepository;
import com.financebuddha.finbud.hrms.service.HierarchyDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HierarchyDashboardServiceImpl implements HierarchyDashboardService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final MonthlyTargetRepository monthlyTargetRepository;
    private final DailyCommitmentRepository dailyCommitmentRepository;

    @Override
    @Transactional(readOnly = true)
    public HierarchyDashboardResponse getMyDashboard(Long employeeId, RoleType tier, Integer year, Integer month) {
        Employee owner = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Employee> reports = resolveDirectReports(employeeId, tier);

        // Batch-load the reports' assigned targets and whole-team disbursal.
        List<Long> reportIds = reports.stream().map(Employee::getId).toList();
        Map<Long, BigDecimal> reportTargets = targetsFor(reportIds, year, month);
        Map<Long, BigDecimal> reportDisbursed = new LinkedHashMap<>();
        Map<Long, Integer> reportTeamSize = new LinkedHashMap<>();
        if (!reportIds.isEmpty()) {
            for (DailyCommitmentRepository.BranchDisbursalRow bRow
                    : dailyCommitmentRepository.aggregateSubtreeDisbursalByBranch(reportIds, start, end)) {
                reportDisbursed.put(bRow.getBranchId(), nz(bRow.getTotal()));
                reportTeamSize.put(bRow.getBranchId(), bRow.getTeamSize() != null ? bRow.getTeamSize() : 0);
            }
        }

        List<HierarchyReportRow> rows = new ArrayList<>();
        BigDecimal teamDisbursed = BigDecimal.ZERO;
        BigDecimal allocated = BigDecimal.ZERO;
        for (Employee r : reports) {
            BigDecimal rTarget = reportTargets.getOrDefault(r.getId(), BigDecimal.ZERO);
            BigDecimal rDisbursed = reportDisbursed.getOrDefault(r.getId(), BigDecimal.ZERO);
            teamDisbursed = teamDisbursed.add(rDisbursed);
            allocated = allocated.add(rTarget);
            rows.add(HierarchyReportRow.builder()
                    .employeeId(r.getId())
                    .employeeCode(r.getEmployeeId())
                    .employeeName(r.getFullName())
                    .assignedTargetDisbursalAmount(rTarget)
                    .teamDisbursedToDate(rDisbursed)
                    .teamSize(reportTeamSize.getOrDefault(r.getId(), 0))
                    .achievedPercent(percent(rDisbursed, rTarget))
                    .build());
        }

        // A leaf (Employee) has no reports — their "team" disbursal is their own.
        if (tier == RoleType.ROLE_EMPLOYEE) {
            teamDisbursed = nz(dailyCommitmentRepository
                    .sumApprovedDisbursalForEmployee(employeeId, start, end));
        }

        BigDecimal myTarget = targetFor(employeeId, year, month);

        return HierarchyDashboardResponse.builder()
                .employeeId(owner.getId())
                .employeeCode(owner.getEmployeeId())
                .employeeName(owner.getFullName())
                .roleLabel(selfLabel(tier))
                .year(year)
                .month(month)
                .myTargetDisbursalAmount(myTarget)
                .teamDisbursedToDate(teamDisbursed)
                .teamAchievedPercent(percent(teamDisbursed, myTarget))
                .allocatedToReports(allocated)
                .unallocatedRemaining(myTarget.subtract(allocated))
                .reportsRoleLabel(reportsLabel(tier))
                .reports(rows)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HierarchyReportRow> getAllEmployeesOverview(Integer year, Integer month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Employee> employees = employeeRepository.findAllByStatus(EmployeeStatus.ACTIVE);
        List<Long> ids = employees.stream().map(Employee::getId).toList();

        Map<Long, BigDecimal> targets = targetsFor(ids, year, month);
        Map<Long, BigDecimal> ownDisbursed = new LinkedHashMap<>();
        dailyCommitmentRepository.aggregateApprovedDisbursalByEmployee(start, end)
                .forEach(row -> ownDisbursed.put(row.getEmployeeId(), nz(row.getTotal())));

        List<HierarchyReportRow> rows = new ArrayList<>();
        for (Employee e : employees) {
            BigDecimal target = targets.getOrDefault(e.getId(), BigDecimal.ZERO);
            BigDecimal disbursed = ownDisbursed.getOrDefault(e.getId(), BigDecimal.ZERO);
            rows.add(HierarchyReportRow.builder()
                    .employeeId(e.getId())
                    .employeeCode(e.getEmployeeId())
                    .employeeName(e.getFullName())
                    .assignedTargetDisbursalAmount(target)
                    .teamDisbursedToDate(disbursed)
                    .achievedPercent(percent(disbursed, target))
                    .build());
        }
        return rows;
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Who sits directly under this node. Admin/HR see the Manager layer (the
     * top of the target chain); everyone else sees their own direct reports via
     * {@code manager_id}. Employees have none.
     */
    private List<Employee> resolveDirectReports(Long employeeId, RoleType tier) {
        return switch (tier) {
            case ROLE_ADMIN, ROLE_HR -> userRepository
                    .findActiveUsersByRoleName(RoleType.ROLE_MANAGER).stream()
                    .map(User::getEmployee)
                    .filter(Objects::nonNull)
                    // De-dupe by employee id, preserving order.
                    .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a, LinkedHashMap::new))
                    .values().stream().toList();
            case ROLE_EMPLOYEE -> List.of();
            default -> employeeRepository.findActiveSubordinates(employeeId);
        };
    }

    private Map<Long, BigDecimal> targetsFor(List<Long> employeeIds, Integer year, Integer month) {
        Map<Long, BigDecimal> map = new LinkedHashMap<>();
        if (employeeIds == null || employeeIds.isEmpty()) {
            return map;
        }
        for (MonthlyTarget t : monthlyTargetRepository
                .findByEmployeeIdInAndYearAndMonth(employeeIds, year, month)) {
            if (t.getEmployee() != null) {
                map.put(t.getEmployee().getId(), nz(t.getTargetDisbursalAmount()));
            }
        }
        return map;
    }


    private BigDecimal targetFor(Long employeeId, Integer year, Integer month) {
        return monthlyTargetRepository.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .map(t -> nz(t.getTargetDisbursalAmount()))
                .orElse(BigDecimal.ZERO);
    }

    private Integer percent(BigDecimal achieved, BigDecimal target) {
        if (target == null || target.signum() <= 0) {
            return 0;
        }
        return nz(achieved)
                .multiply(BigDecimal.valueOf(100))
                .divide(target, 0, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100))
                .intValue();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String selfLabel(RoleType tier) {
        return switch (tier) {
            case ROLE_ADMIN -> "Admin";
            case ROLE_HR -> "HR";
            case ROLE_MANAGER -> "Manager";
            case ROLE_TEAM_LEADER -> "Team Leader";
            case ROLE_ATL -> "ATL";
            case ROLE_EMPLOYEE -> "Employee";
        };
    }

    private String reportsLabel(RoleType tier) {
        return switch (tier) {
            case ROLE_ADMIN, ROLE_HR -> "Manager";
            case ROLE_MANAGER -> "Team Leader";
            case ROLE_TEAM_LEADER -> "ATL";
            case ROLE_ATL -> "Employee";
            case ROLE_EMPLOYEE -> "";
        };
    }
}
