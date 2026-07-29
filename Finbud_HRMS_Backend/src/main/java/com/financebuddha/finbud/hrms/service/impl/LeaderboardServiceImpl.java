package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.commitment.LeaderboardEntryResponse;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.repository.DailyCommitmentRepository;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {

    private final DailyCommitmentRepository dailyCommitmentRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> monthlyDisbursal(Integer year, Integer month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<DailyCommitmentRepository.LeaderboardRow> raw =
                dailyCommitmentRepository.aggregateApprovedDisbursalByEmployee(start, end);

        // Single batched fetch of all relevant employees so we don't N+1 the
        // employee table. The Map lookup keeps the response build path
        // proportional to the result-set size, not the employee count.
        Map<Long, Employee> byId = employeeRepository
                .findAllById(raw.stream().map(DailyCommitmentRepository.LeaderboardRow::getEmployeeId).toList())
                .stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));

        AtomicInteger rank = new AtomicInteger(0);
        return raw.stream()
                .map(row -> {
                    Employee e = byId.get(row.getEmployeeId());
                    return LeaderboardEntryResponse.builder()
                            .rank(rank.incrementAndGet())
                            .employeeId(row.getEmployeeId())
                            .employeeCode(e != null ? e.getEmployeeId() : null)
                            .employeeName(e != null ? e.getFullName() : null)
                            .department(e != null && e.getDepartment() != null ? e.getDepartment().getName() : null)
                            .totalDisbursalAmount(row.getTotal())
                            .build();
                })
                .toList();
    }
}
