package com.financebuddha.finbud.hrms.service;

import java.io.IOException;
import java.time.LocalDate;

public interface CommitmentReportService {

    /**
     * Build a daily commitment Excel report for ONE employee in a date
     * window. Returns the raw .xlsx bytes ready to be written to the
     * response. Each row is one work_date with targets, actuals, %, status.
     */
    byte[] employeeDailyXlsx(Long employeeId, LocalDate startDate, LocalDate endDate) throws IOException;

    /**
     * Build a daily commitment Excel report for a manager's TEAM in a
     * date window. Each row is (date, employee, targets, actuals, status).
     */
    byte[] teamDailyXlsx(Long managerId, LocalDate startDate, LocalDate endDate) throws IOException;
}
