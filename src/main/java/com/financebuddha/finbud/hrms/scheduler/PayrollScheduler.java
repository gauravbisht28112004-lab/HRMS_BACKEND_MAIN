package com.financebuddha.finbud.hrms.scheduler;

import com.financebuddha.finbud.hrms.service.PayrollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayrollScheduler {

    private final PayrollService payrollService;

    // Run at 1:00 AM on the 1st day of every month
    @Scheduled(cron = "${app.scheduler.payroll.cron:0 0 1 1 * ?}")
    public void generateMonthlyPayroll() {
        log.info("Starting automatic monthly payroll generation");
        try {
            payrollService.autoGenerateMonthlyPayroll();
            log.info("Monthly payroll generation completed successfully");
        } catch (Exception e) {
            log.error("Error during monthly payroll generation: {}", e.getMessage(), e);
        }
    }
}
