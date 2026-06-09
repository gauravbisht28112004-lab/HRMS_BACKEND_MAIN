package com.financebuddha.finbud.hrms.scheduler;

import com.financebuddha.finbud.hrms.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Nightly / end-of-day attendance housekeeping jobs. Schedules are configurable
 * via application properties so staging can run them less aggressively:
 *
 * <ul>
 *     <li>{@code app.scheduler.attendance.auto-absent.cron} — default 00:30 IST, runs for <em>yesterday</em></li>
 *     <li>{@code app.scheduler.attendance.missing-punch.cron} — default 21:00 IST, runs for <em>today</em></li>
 * </ul>
 *
 * <p>Both jobs are idempotent and safe to re-run.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceScheduler {

    private final AttendanceService attendanceService;

    /**
     * Auto-mark Absent for active employees who did not punch on the previous
     * calendar day. Runs at 00:30 IST ("0 30 0 * * ?" in Asia/Kolkata).
     */
    @Scheduled(
            cron = "${app.scheduler.attendance.auto-absent.cron:0 30 0 * * ?}",
            zone = "${app.scheduler.timezone:Asia/Kolkata}"
    )
    public void autoMarkAbsent() {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        log.info("Running auto-mark-absent job for {}", targetDate);
        try {
            int written = attendanceService.autoMarkAbsentForDate(targetDate);
            log.info("auto-mark-absent job completed: {} rows written for {}", written, targetDate);
        } catch (Exception e) {
            log.error("auto-mark-absent job failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Flag any open punch-in that never had a corresponding punch-out as
     * MISSING_PUNCH and send the row back to PENDING for review. Runs at
     * 21:00 IST so it catches evening shifts after they would have ended.
     */
    @Scheduled(
            cron = "${app.scheduler.attendance.missing-punch.cron:0 0 21 * * ?}",
            zone = "${app.scheduler.timezone:Asia/Kolkata}"
    )
    public void autoCloseMissingPunches() {
        LocalDate today = LocalDate.now();
        log.info("Running auto-close-missing-punches job for {}", today);
        try {
            int updated = attendanceService.autoCloseMissingPunchesForDate(today);
            log.info("auto-close-missing-punches job completed: {} rows flagged for {}", updated, today);
        } catch (Exception e) {
            log.error("auto-close-missing-punches job failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Legacy end-of-day hook retained for backwards compatibility with any
     * existing deployment pipeline that schedules via the old property key.
     * Running this is a safe no-op superset of the targeted jobs above.
     */
    @Scheduled(
            cron = "${app.scheduler.attendance.cron:0 59 23 * * ?}",
            zone = "${app.scheduler.timezone:Asia/Kolkata}"
    )
    public void processDailyAttendance() {
        LocalDate today = LocalDate.now();
        log.info("Running daily attendance housekeeping for {}", today);
        try {
            attendanceService.processDailyAttendance(today);
        } catch (Exception e) {
            log.error("daily attendance housekeeping failed: {}", e.getMessage(), e);
        }
    }
}
