package com.financebuddha.finbud.hrms.scheduler;

import com.financebuddha.finbud.hrms.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceScheduler {

    private final AttendanceService attendanceService;

    // Run at 11:59 PM every day
    @Scheduled(cron = "${app.scheduler.attendance.cron:0 59 23 * * ?}")
    public void processDailyAttendance() {
        log.info("Starting daily attendance processing");
        try {
            LocalDate today = LocalDate.now();
            attendanceService.processDailyAttendance(today);
            log.info("Daily attendance processing completed for {}", today);
        } catch (Exception e) {
            log.error("Error during daily attendance processing: {}", e.getMessage(), e);
        }
    }
}
