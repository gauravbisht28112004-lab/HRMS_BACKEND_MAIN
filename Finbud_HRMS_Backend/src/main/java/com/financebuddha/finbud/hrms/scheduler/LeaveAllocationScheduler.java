package com.financebuddha.finbud.hrms.scheduler;

import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.LeaveBalance;
import com.financebuddha.finbud.hrms.enums.EmployeeStatus;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.LeaveBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Yearly leave-balance allocation job.
 *
 * <p>Runs at <b>00:00 IST on January&nbsp;1</b> of each calendar year. For
 * every ACTIVE employee, it:
 *
 * <ol>
 *   <li>Reads last year's balance (may not exist — employee joined this year,
 *       balance was never initialised, etc.).</li>
 *   <li>Computes the carry-forward <em>per leave type</em> as
 *       {@code min(unused_last_year, annual_allocation)}. Excess lapses
 *       (Finbud policy decision, 2026-04-24).</li>
 *   <li>Creates the new year's {@link LeaveBalance} row with
 *       {@code allocated = annual + carried_forward}. Someone who used
 *       nothing in 2025 therefore starts 2026 with {@code 12} casual+sick
 *       (6 fresh + 6 carried) and {@code 12} paid.</li>
 *   <li>Skips employees who already have a balance for the new year —
 *       makes the job idempotent across server restarts / manual re-runs.</li>
 * </ol>
 *
 * <p>New joiners mid-year get <b>full annual allocation</b> (not pro-rated)
 * per policy — that happens elsewhere at employee-create time, not here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveAllocationScheduler {

    /** Finbud annual allocation: casual + sick share this pool. */
    private static final BigDecimal ANNUAL_CASUAL_SICK = new BigDecimal("6.00");
    /** Finbud annual allocation: paid / earned. */
    private static final BigDecimal ANNUAL_PAID = new BigDecimal("6.00");

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    /**
     * Cron: {@code 0 0 0 1 1 *} (sec min hour day month day-of-week) in IST —
     * midnight on 1 January. Spring will schedule the next fire regardless of
     * how long the app has been running.
     */
    @Scheduled(cron = "0 0 0 1 1 *", zone = "Asia/Kolkata")
    @Transactional
    public void allocateYearlyLeaveBalances() {
        int newYear = LocalDate.now(IST).getYear();
        int lastYear = newYear - 1;
        log.info("Yearly leave allocation starting: seeding balances for year={} from year={}", newYear, lastYear);

        List<Employee> activeEmployees = employeeRepository.findAllByStatus(EmployeeStatus.ACTIVE);
        int created = 0;
        int skipped = 0;

        for (Employee employee : activeEmployees) {
            try {
                boolean outcome = allocateForEmployee(employee, newYear, lastYear);
                if (outcome) created++;
                else skipped++;
            } catch (Exception ex) {
                // Never let one bad row stop the whole job. The employee can
                // be re-run manually via the initialize endpoint if needed.
                log.error("Yearly allocation failed for employee id={}, employeeId={}: {}",
                        employee.getId(), employee.getEmployeeId(), ex.getMessage(), ex);
            }
        }

        log.info("Yearly leave allocation complete: {} created, {} skipped (already existed) out of {} active employees",
                created, skipped, activeEmployees.size());
    }

    /**
     * Allocates one employee's balance for {@code newYear}. Returns
     * {@code true} if a new row was persisted, {@code false} if a balance
     * already existed and we left it alone.
     */
    private boolean allocateForEmployee(Employee employee, int newYear, int lastYear) {
        if (leaveBalanceRepository.existsByEmployeeIdAndYear(employee.getId(), newYear)) {
            return false;
        }

        BigDecimal casualSickCarried = BigDecimal.ZERO;
        BigDecimal paidCarried = BigDecimal.ZERO;

        LeaveBalance previous = leaveBalanceRepository
                .findByEmployeeIdAndYear(employee.getId(), lastYear)
                .orElse(null);

        if (previous != null) {
            // Carry forward at most one full annual allocation. Negative
            // balances can happen if someone is in LOP territory — clamp to
            // zero so we never "un-allocate" days.
            casualSickCarried = clamp(previous.getCasualSickBalance(), BigDecimal.ZERO, ANNUAL_CASUAL_SICK);
            paidCarried = clamp(previous.getPaidLeaveBalance(), BigDecimal.ZERO, ANNUAL_PAID);
        }

        LeaveBalance newBalance = LeaveBalance.builder()
                .employee(employee)
                .year(newYear)
                .casualSickAllocated(ANNUAL_CASUAL_SICK.add(casualSickCarried))
                .casualSickUsed(BigDecimal.ZERO)
                .casualSickCarriedForward(casualSickCarried)
                .paidLeaveAllocated(ANNUAL_PAID.add(paidCarried))
                .paidLeaveUsed(BigDecimal.ZERO)
                .paidLeaveCarriedForward(paidCarried)
                .lopDays(BigDecimal.ZERO)
                .build();

        leaveBalanceRepository.save(newBalance);
        return true;
    }

    private static BigDecimal clamp(BigDecimal value, BigDecimal lo, BigDecimal hi) {
        if (value == null) return lo;
        if (value.compareTo(lo) < 0) return lo;
        if (value.compareTo(hi) > 0) return hi;
        return value;
    }
}
