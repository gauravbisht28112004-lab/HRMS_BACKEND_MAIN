package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.attendance.AttendanceApprovalRequest;
import com.financebuddha.finbud.hrms.dto.attendance.AttendanceManualEntryRequest;
import com.financebuddha.finbud.hrms.dto.attendance.AttendanceResponse;
import com.financebuddha.finbud.hrms.dto.attendance.AttendanceSummaryResponse;
import com.financebuddha.finbud.hrms.dto.attendance.PunchRequest;
import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.security.CurrentUser;
import com.financebuddha.finbud.hrms.security.UserPrincipal;
import com.financebuddha.finbud.hrms.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Portal punch flow, approvals, and history")
@SecurityRequirement(name = "bearerAuth")
public class AttendanceController {

    private final AttendanceService attendanceService;

    // ==================== Portal punch flow (self-service) ====================

    @PostMapping("/punch-in")
    @Operation(summary = "Punch in",
            description = "Record punch-in for the currently authenticated employee. Creates an attendance row in PENDING state.")
    public ResponseEntity<ApiResponse<AttendanceResponse>> recordPunchIn(
            @Valid @RequestBody(required = false) PunchRequest request,
            @CurrentUser UserPrincipal principal) {
        PunchRequest payload = request != null ? request : new PunchRequest();
        AttendanceResponse response = attendanceService.recordPunchIn(payload, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Punch in recorded", response));
    }

    @PostMapping("/punch-out")
    @Operation(summary = "Punch out", description = "Record punch-out for the currently authenticated employee")
    public ResponseEntity<ApiResponse<AttendanceResponse>> recordPunchOut(
            @Valid @RequestBody(required = false) PunchRequest request,
            @CurrentUser UserPrincipal principal) {
        PunchRequest payload = request != null ? request : new PunchRequest();
        AttendanceResponse response = attendanceService.recordPunchOut(payload, principal);
        return ResponseEntity.ok(ApiResponse.success("Punch out recorded", response));
    }

    // ==================== Approval workflow (TL / HR / Admin) ==================

    @GetMapping("/approvals/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Pending approvals",
            description = "HR/Admin see the full queue; Manager sees their direct reports only")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getPendingApprovals(
            @CurrentUser UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getPendingApprovals(principal)));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Approve or reject attendance row",
            description = "Manager can only act on direct-report rows. Rejection requires a reason.")
    public ResponseEntity<ApiResponse<AttendanceResponse>> review(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceApprovalRequest request,
            @CurrentUser UserPrincipal principal) {
        AttendanceResponse response = attendanceService.reviewAttendance(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Attendance reviewed", response));
    }

    @PostMapping("/manual-entry")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "HR / Admin manual entry",
            description = "Record or override an attendance row; auto-approves and flags manuallyEditedBy")
    public ResponseEntity<ApiResponse<AttendanceResponse>> manualEntry(
            @Valid @RequestBody AttendanceManualEntryRequest request,
            @CurrentUser UserPrincipal principal) {
        AttendanceResponse response = attendanceService.manualEntry(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Manual attendance entry saved", response));
    }

    // ==================== Read / query APIs ====================

    @GetMapping("/{id}")
    @Operation(summary = "Get attendance by id")
    public ResponseEntity<ApiResponse<AttendanceResponse>> getAttendanceById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getAttendanceById(id)));
    }

    @GetMapping("/employee/{employeeId}/date/{date}")
    @Operation(summary = "Get attendance for employee and date")
    public ResponseEntity<ApiResponse<AttendanceResponse>> getAttendanceByEmployeeAndDate(
            @PathVariable Long employeeId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.getAttendanceByEmployeeAndDate(employeeId, date)));
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Paginated attendance for an employee")
    public ResponseEntity<ApiResponse<PagedResponse<AttendanceResponse>>> getAttendanceByEmployee(
            @PathVariable Long employeeId,
            @ParameterObject PaginationRequest paginationRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.getAttendanceByEmployee(employeeId, paginationRequest)));
    }

    @GetMapping("/employee/{employeeId}/range")
    @Operation(summary = "Attendance by date range")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getAttendanceByEmployeeAndDateRange(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.getAttendanceByEmployeeAndDateRange(employeeId, startDate, endDate)));
    }

    @GetMapping("/late-comers/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Late comers for a date")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getLateComersByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getLateComersByDate(date)));
    }

    @GetMapping("/absent/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Absent employees for a date")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getAbsentByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.getAbsentByDate(date)));
    }

    @GetMapping("/overtime")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Overtime rows for a date range")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getOvertimeByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.getOvertimeByDateRange(startDate, endDate)));
    }

    @GetMapping("/employee/{employeeId}/summary")
    @Operation(summary = "Monthly attendance summary for an employee")
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>> getAttendanceSummary(
            @PathVariable Long employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(ApiResponse.success(
                attendanceService.getAttendanceSummary(employeeId, month, year)));
    }

    @GetMapping("/employee/{employeeId}/punched-in-today")
    @Operation(summary = "Has employee punched in today?")
    public ResponseEntity<ApiResponse<Boolean>> hasPunchedInToday(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.hasPunchedInToday(employeeId)));
    }

    @GetMapping("/employee/{employeeId}/punched-out-today")
    @Operation(summary = "Has employee punched out today?")
    public ResponseEntity<ApiResponse<Boolean>> hasPunchedOutToday(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success(attendanceService.hasPunchedOutToday(employeeId)));
    }
}
