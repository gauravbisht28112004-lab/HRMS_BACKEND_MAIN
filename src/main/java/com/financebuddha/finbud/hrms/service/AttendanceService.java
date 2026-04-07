package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.attendance.AttendanceResponse;
import com.financebuddha.finbud.hrms.dto.attendance.AttendanceSummaryResponse;
import com.financebuddha.finbud.hrms.dto.attendance.PunchRequest;
import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    AttendanceResponse recordPunchIn(PunchRequest request);

    AttendanceResponse recordPunchOut(PunchRequest request);

    AttendanceResponse getAttendanceById(Long id);

    AttendanceResponse getAttendanceByEmployeeAndDate(Long employeeId, LocalDate date);

    PagedResponse<AttendanceResponse> getAttendanceByEmployee(Long employeeId, PaginationRequest paginationRequest);

    List<AttendanceResponse> getAttendanceByEmployeeAndDateRange(Long employeeId, LocalDate startDate, LocalDate endDate);

    List<AttendanceResponse> getLateComersByDate(LocalDate date);

    List<AttendanceResponse> getAbsentByDate(LocalDate date);

    List<AttendanceResponse> getOvertimeByDateRange(LocalDate startDate, LocalDate endDate);

    AttendanceSummaryResponse getAttendanceSummary(Long employeeId, int month, int year);

    void processDailyAttendance(LocalDate date);

    boolean hasPunchedInToday(Long employeeId);

    boolean hasPunchedOutToday(Long employeeId);
}
