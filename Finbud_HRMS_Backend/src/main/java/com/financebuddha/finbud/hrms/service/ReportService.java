package com.financebuddha.finbud.hrms.service;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Excel report builder for the admin Reports dashboard. Each method returns
 * the raw .xlsx bytes ready to be streamed back as a download — the same
 * contract the commitment reports use (see {@link CommitmentReportService}).
 *
 * <p>Both reports accept a date window and an optional {@code departmentId}
 * filter ({@code null} = all departments).
 */
public interface ReportService {

    /** Leave requests overlapping the window: type, span, days, status, approver. */
    byte[] leaveXlsx(LocalDate startDate, LocalDate endDate, Long departmentId) throws IOException;

    /**
     * Payroll rows whose (year, month) falls within the window: earnings,
     * deductions, net pay, overtime.
     */
    byte[] payrollXlsx(LocalDate startDate, LocalDate endDate, Long departmentId) throws IOException;
}
