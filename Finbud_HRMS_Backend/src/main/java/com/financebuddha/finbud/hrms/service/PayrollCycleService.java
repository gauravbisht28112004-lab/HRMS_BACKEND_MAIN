package com.financebuddha.finbud.hrms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Single source of truth for the company pay cycle.
 *
 * <p>Finbud does NOT run on calendar months. A cycle starts on
 * {@code app.payroll.cycle-start-day} (default 25) and ends on the day before
 * the next cycle-start-day. A payroll labelled {@code (year, month)} is named
 * after the month it is PAID FOR — i.e. the month it ENDS in. With start day 25:
 *
 * <pre>
 *   label June 2026  -&gt;  25 May 2026 .. 24 Jun 2026   (both inclusive)
 *   label July 2026  -&gt;  25 Jun 2026 .. 24 Jul 2026
 * </pre>
 *
 * <p>Keep {@code cycle-start-day} in the range 1..28 so the same day-of-month
 * exists in every month (no February edge cases). Setting it to {@code 1}
 * restores the original calendar-month behaviour exactly.
 */
@Component
public class PayrollCycleService {

    private final int cycleStartDay;

    public PayrollCycleService(
            @Value("${app.payroll.cycle-start-day:25}") int cycleStartDay) {
        if (cycleStartDay < 1 || cycleStartDay > 28) {
            throw new IllegalStateException(
                    "app.payroll.cycle-start-day must be between 1 and 28, was " + cycleStartDay);
        }
        this.cycleStartDay = cycleStartDay;
    }

    /** First calendar date (inclusive) of the cycle labelled (year, month). */
    public LocalDate cycleStart(int year, int month) {
        if (cycleStartDay == 1) {
            return LocalDate.of(year, month, 1);
        }
        // cycle-start-day of the month BEFORE the labelled month (e.g. 25 May for June).
        return LocalDate.of(year, month, 1).minusMonths(1).withDayOfMonth(cycleStartDay);
    }

    /** Last calendar date (inclusive) of the cycle labelled (year, month). */
    public LocalDate cycleEnd(int year, int month) {
        if (cycleStartDay == 1) {
            return LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
        }
        // Day before the labelled month's start day (e.g. 24 Jun for June).
        return LocalDate.of(year, month, 1).withDayOfMonth(cycleStartDay).minusDays(1);
    }

    public int getCycleStartDay() {
        return cycleStartDay;
    }

    /**
     * The cycle label a given calendar date belongs to. A date on or after the
     * cycle-start-day rolls into NEXT month's label. With start day 25:
     * 25 May -&gt; June, 24 Jun -&gt; June, 25 Jun -&gt; July.
     */
    public YearMonth labelFor(LocalDate date) {
        YearMonth ym = YearMonth.from(date);
        if (cycleStartDay == 1) {
            return ym;
        }
        return date.getDayOfMonth() >= cycleStartDay ? ym.plusMonths(1) : ym;
    }

    /**
     * The most recently CLOSED cycle as of {@code today} — the one to run
     * payroll for. On 25 Jun this returns June (25 May..24 Jun), which closed
     * on the 24th.
     */
    public YearMonth mostRecentlyClosedCycle(LocalDate today) {
        return labelFor(today).minusMonths(1);
    }
}
