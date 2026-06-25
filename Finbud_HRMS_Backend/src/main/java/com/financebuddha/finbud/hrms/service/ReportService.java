package com.financebuddha.finbud.hrms.service;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Excel report builder for the admin Reports dashboard. Each method returns
 * the raw .xlsx bytes ready to be streamed back as a download — the same
 * contract the commitment reports use (see {@link CommitmentReportService}).
 *
 * <p>All four reports accept a date window and an optional {@code departmentId}
 * filter ({@code null} = all departments).
 */
public interface ReportService {

    /** Daily attendance rows: status, punches, hours, late / half-day / overtime flags. */
    byte[] attendanceXlsx(LocalDate startDate, LocalDate endDate, Long departmentId) throws IOException;

    /** Leave requests overlapping the window: type, span, days, status, approver. */
    byte[] leaveXlsx(LocalDate startDate, LocalDate endDate, Long departmentId) throws IOException;

    /**
     * Payroll rows whose (year, month) falls within the window: earnings,
     * deductions, net pay, overtime.
     */
    byte[] payrollXlsx(LocalDate startDate, LocalDate endDate, Long departmentId) throws IOException;

    /** Overtime-only attendance rows: overtime hours, shift, working hours. */
    byte[] overtimeXlsx(LocalDate startDate, LocalDate endDate, Long departmentId) throws IOException;
}
