package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.attendance.AttendanceApprovalRequest;
import com.financebuddha.finbud.hrms.dto.attendance.AttendanceManualEntryRequest;
import com.financebuddha.finbud.hrms.dto.attendance.AttendanceResponse;
import com.financebuddha.finbud.hrms.dto.attendance.AttendanceSummaryResponse;
import com.financebuddha.finbud.hrms.dto.attendance.PunchRequest;
import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.security.UserPrincipal;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    /* --------------------------- Portal punch flow --------------------------- */

    /** Punch in for the currently authenticated employee. */
    AttendanceResponse recordPunchIn(PunchRequest request, UserPrincipal principal);

    /** Punch out for the currently authenticated employee. */
    AttendanceResponse recordPunchOut(PunchRequest request, UserPrincipal principal);

    /* --------------------------- Approval workflow --------------------------- */

    /** Approval queue visible to the caller (HR/Admin = org-wide, TL = direct reports). */
    List<AttendanceResponse> getPendingApprovals(UserPrincipal principal);

    /** Approve or reject a single attendance row. */
    AttendanceResponse reviewAttendance(Long attendanceId, AttendanceApprovalRequest request, UserPrincipal principal);

    /** HR / Admin manual entry or override. */
    AttendanceResponse manualEntry(AttendanceManualEntryRequest request, UserPrincipal principal);

    /* --------------------------- Read / query APIs --------------------------- */

    AttendanceResponse getAttendanceById(Long id);

    AttendanceResponse getAttendanceByEmployeeAndDate(Long employeeId, LocalDate date);

    PagedResponse<AttendanceResponse> getAttendanceByEmployee(Long employeeId, PaginationRequest paginationRequest);

    List<AttendanceResponse> getAttendanceByEmployeeAndDateRange(Long employeeId, LocalDate startDate, LocalDate endDate);

    List<AttendanceResponse> getLateComersByDate(LocalDate date);

    List<AttendanceResponse> getAbsentByDate(LocalDate date);

    List<AttendanceResponse> getOvertimeByDateRange(LocalDate startDate, LocalDate endDate);

    AttendanceSummaryResponse getAttendanceSummary(Long employeeId, int month, int year);

    /* --------------------------- Scheduler hooks --------------------------- */

    /** Create AUTO_ABSENT rows for active employees with no punch on the given date. */
    int autoMarkAbsentForDate(LocalDate date);

    /** Flag as MISSING_PUNCH any open punch-in with no punch-out past shift end. */
    int autoCloseMissingPunchesForDate(LocalDate date);

    /* Kept for backwards compatibility with the existing scheduler wiring. */
    void processDailyAttendance(LocalDate date);

    boolean hasPunchedInToday(Long employeeId);

    boolean hasPunchedOutToday(Long employeeId);
}
