package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.attendance.AttendanceResponse;
import com.financebuddha.finbud.hrms.dto.attendance.AttendanceSummaryResponse;
import com.financebuddha.finbud.hrms.dto.attendance.PunchRequest;
import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Attendance management APIs")
@SecurityRequirement(name = "bearerAuth")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/punch-in")
    @Operation(summary = "Record punch in", description = "Record employee punch in (for fingerprint devices)")
    public ResponseEntity<ApiResponse<AttendanceResponse>> recordPunchIn(@Valid @RequestBody PunchRequest request) {
        AttendanceResponse response = attendanceService.recordPunchIn(request);
        return ResponseEntity.ok(ApiResponse.success("Punch in recorded", response));
    }

    @PostMapping("/punch-out")
    @Operation(summary = "Record punch out", description = "Record employee punch out (for fingerprint devices)")
    public ResponseEntity<ApiResponse<AttendanceResponse>> recordPunchOut(@Valid @RequestBody PunchRequest request) {
        AttendanceResponse response = attendanceService.recordPunchOut(request);
        return ResponseEntity.ok(ApiResponse.success("Punch out recorded", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get attendance by ID", description = "Get attendance record by ID")
    public ResponseEntity<ApiResponse<AttendanceResponse>> getAttendanceById(@PathVariable Long id) {
        AttendanceResponse response = attendanceService.getAttendanceById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/employee/{employeeId}/date/{date}")
    @Operation(summary = "Get attendance by date", description = "Get attendance for employee on specific date")
    public ResponseEntity<ApiResponse<AttendanceResponse>> getAttendanceByEmployeeAndDate(
            @PathVariable Long employeeId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AttendanceResponse response = attendanceService.getAttendanceByEmployeeAndDate(employeeId, date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get employee attendance", description = "Get paginated attendance for an employee")
    public ResponseEntity<ApiResponse<PagedResponse<AttendanceResponse>>> getAttendanceByEmployee(
            @PathVariable Long employeeId,
            @ParameterObject PaginationRequest paginationRequest) {
        PagedResponse<AttendanceResponse> response = attendanceService.getAttendanceByEmployee(employeeId, paginationRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/employee/{employeeId}/range")
    @Operation(summary = "Get attendance by date range", description = "Get attendance for date range")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getAttendanceByEmployeeAndDateRange(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AttendanceResponse> response = attendanceService.getAttendanceByEmployeeAndDateRange(employeeId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/late-comers/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Get late comers", description = "Get list of employees who came late on specific date")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getLateComersByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceResponse> response = attendanceService.getLateComersByDate(date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/absent/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Get absent employees", description = "Get list of absent employees on specific date")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getAbsentByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceResponse> response = attendanceService.getAbsentByDate(date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/overtime")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Get overtime records", description = "Get overtime records for date range")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getOvertimeByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AttendanceResponse> response = attendanceService.getOvertimeByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/employee/{employeeId}/summary")
    @Operation(summary = "Get attendance summary", description = "Get monthly attendance summary")
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>> getAttendanceSummary(
            @PathVariable Long employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        AttendanceSummaryResponse response = attendanceService.getAttendanceSummary(employeeId, month, year);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/employee/{employeeId}/punched-in-today")
    @Operation(summary = "Check punch in status", description = "Check if employee has punched in today")
    public ResponseEntity<ApiResponse<Boolean>> hasPunchedInToday(@PathVariable Long employeeId) {
        boolean punchedIn = attendanceService.hasPunchedInToday(employeeId);
        return ResponseEntity.ok(ApiResponse.success(punchedIn));
    }

    @GetMapping("/employee/{employeeId}/punched-out-today")
    @Operation(summary = "Check punch out status", description = "Check if employee has punched out today")
    public ResponseEntity<ApiResponse<Boolean>> hasPunchedOutToday(@PathVariable Long employeeId) {
        boolean punchedOut = attendanceService.hasPunchedOutToday(employeeId);
        return ResponseEntity.ok(ApiResponse.success(punchedOut));
    }
}
