package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.commitment.MonthlyTargetResponse;
import com.financebuddha.finbud.hrms.dto.commitment.MonthlyTargetUpsertRequest;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.MonthlyTarget;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.repository.DailyCommitmentRepository;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.MonthlyTargetRepository;
import com.financebuddha.finbud.hrms.security.AuthzService;
import com.financebuddha.finbud.hrms.service.MonthlyTargetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyTargetServiceImpl implements MonthlyTargetService {

    private final MonthlyTargetRepository monthlyTargetRepository;
    private final EmployeeRepository employeeRepository;
    private final DailyCommitmentRepository dailyCommitmentRepository;
    private final AuthzService authzService;

    @Override
    @Transactional
    public MonthlyTargetResponse upsert(Long employeeId, Long setterEmployeeId, MonthlyTargetUpsertRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
        Employee setter = employeeRepository.findById(setterEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", setterEmployeeId));

        // Defense-in-depth: an ATL may only assign targets to their own direct
        // reports; ADMIN/HR/MANAGER are unrestricted.
        authzService.requireCanManageEmployee(employee);

        MonthlyTarget target = monthlyTargetRepository
                .findByEmployeeIdAndYearAndMonth(employeeId, request.getYear(), request.getMonth())
                .orElseGet(() -> MonthlyTarget.builder()
                        .employee(employee)
                        .year(request.getYear())
                        .month(request.getMonth())
                        .build());

        target.setTargetDisbursalAmount(request.getTargetDisbursalAmount());
        target.setTargetLogins(request.getTargetLogins());
        target.setSetBy(setter);
        target.setNotes(request.getNotes());

        MonthlyTarget saved = monthlyTargetRepository.save(target);
        log.info("Monthly target set: employee={}, period={}/{}, by={}",
                employeeId, request.getMonth(), request.getYear(), setterEmployeeId);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyTargetResponse get(Long employeeId, Integer year, Integer month) {
        return monthlyTargetRepository.findByEmployeeIdAndYearAndMonth(employeeId, year, month)
                .map(this::toResponse)
                // Return a zero-target placeholder rather than 404 — the UI
                // can render "no target set yet" without an extra status check.
                .orElseGet(() -> {
                    Employee employee = employeeRepository.findById(employeeId)
                            .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));
                    return MonthlyTargetResponse.builder()
                            .employeeId(employee.getId())
                            .employeeCode(employee.getEmployeeId())
                            .employeeName(employee.getFullName())
                            .year(year)
                            .month(month)
                            .targetDisbursalAmount(BigDecimal.ZERO)
                            .targetLogins(0)
                            .achievedDisbursalAmount(achievedFor(employeeId, year, month))
                            .achievedPercent(0)
                            .build();
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlyTargetResponse> listForManager(Long managerId, Integer year, Integer month) {
        return monthlyTargetRepository.findByManagerAndPeriod(managerId, year, month)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Read-time aggregate: sum of APPROVED daily disbursal for the
     * employee in the given (year, month). 0 when nothing is approved yet.
     */
    private BigDecimal achievedFor(Long employeeId, Integer year, Integer month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        BigDecimal sum = dailyCommitmentRepository
                .sumApprovedDisbursalForEmployee(employeeId, start, end);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private MonthlyTargetResponse toResponse(MonthlyTarget t) {
        BigDecimal achieved = achievedFor(t.getEmployee().getId(), t.getYear(), t.getMonth());
        BigDecimal targetAmount = t.getTargetDisbursalAmount() != null
                ? t.getTargetDisbursalAmount() : BigDecimal.ZERO;
        int pct = 0;
        if (targetAmount.signum() > 0) {
            pct = achieved
                    .multiply(BigDecimal.valueOf(100))
                    .divide(targetAmount, 0, RoundingMode.HALF_UP)
                    .min(BigDecimal.valueOf(100))
                    .intValue();
        }

        Employee employee = t.getEmployee();
        Employee setter = t.getSetBy();
        return MonthlyTargetResponse.builder()
                .id(t.getId())
                .employeeId(employee != null ? employee.getId() : null)
                .employeeCode(employee != null ? employee.getEmployeeId() : null)
                .employeeName(employee != null ? employee.getFullName() : null)
                .year(t.getYear())
                .month(t.getMonth())
                .targetDisbursalAmount(targetAmount)
                .targetLogins(t.getTargetLogins() != null ? t.getTargetLogins() : 0)
                .achievedDisbursalAmount(achieved)
                .achievedPercent(pct)
                .setById(setter != null ? setter.getId() : null)
                .setByName(setter != null ? setter.getFullName() : null)
                .notes(t.getNotes())
                .build();
    }
}
