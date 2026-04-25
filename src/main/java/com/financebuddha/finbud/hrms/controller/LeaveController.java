package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.dto.leave.LeaveApprovalRequest;
import com.financebuddha.finbud.hrms.dto.leave.LeaveBalanceAdjustmentRequest;
import com.financebuddha.finbud.hrms.dto.leave.LeaveBalanceResponse;
import com.financebuddha.finbud.hrms.dto.leave.LeaveOverrideRequest;
import com.financebuddha.finbud.hrms.dto.leave.LeaveRequestDTO;
import com.financebuddha.finbud.hrms.dto.leave.LeaveResponse;
import com.financebuddha.finbud.hrms.enums.LeaveStatus;
import com.financebuddha.finbud.hrms.security.CurrentUser;
import com.financebuddha.finbud.hrms.security.UserPrincipal;
import com.financebuddha.finbud.hrms.service.LeaveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
@Tag(name = "Leaves", description = "Leave management APIs")
@SecurityRequirement(name = "bearerAuth")
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping("/apply")
    @Operation(summary = "Apply for leave", description = "Apply for leave")
    public ResponseEntity<ApiResponse<LeaveResponse>> applyLeave(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody LeaveRequestDTO request) {
        LeaveResponse response = leaveService.applyLeave(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Leave application submitted", response));
    }

    @PostMapping("/{leaveRequestId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Approve leave", description = "Approve a pending leave request")
    public ResponseEntity<ApiResponse<LeaveResponse>> approveLeave(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long leaveRequestId,
            @Valid @RequestBody LeaveApprovalRequest request) {
        LeaveResponse response = leaveService.approveLeave(leaveRequestId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Leave approved", response));
    }

    @PostMapping("/{leaveRequestId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Reject leave", description = "Reject a pending leave request")
    public ResponseEntity<ApiResponse<LeaveResponse>> rejectLeave(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long leaveRequestId,
            @RequestParam String reason) {
        LeaveResponse response = leaveService.rejectLeave(leaveRequestId, currentUser.getId(), reason);
        return ResponseEntity.ok(ApiResponse.success("Leave rejected", response));
    }

    @PostMapping("/{leaveRequestId}/cancel")
    @Operation(summary = "Cancel leave", description = "Cancel a leave request")
    public ResponseEntity<ApiResponse<LeaveResponse>> cancelLeave(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long leaveRequestId) {
        LeaveResponse response = leaveService.cancelLeave(leaveRequestId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Leave cancelled", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get leave by ID", description = "Get leave request details by ID")
    public ResponseEntity<ApiResponse<LeaveResponse>> getLeaveById(@PathVariable Long id) {
        LeaveResponse response = leaveService.getLeaveById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get employee leaves", description = "Get paginated leave history for an employee")
    public ResponseEntity<ApiResponse<PagedResponse<LeaveResponse>>> getLeavesByEmployee(
            @PathVariable Long employeeId,
            @ParameterObject PaginationRequest paginationRequest) {
        PagedResponse<LeaveResponse> response = leaveService.getLeavesByEmployee(employeeId, paginationRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/manager/{managerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Get leaves by manager", description = "Get leave requests for manager approval")
    public ResponseEntity<ApiResponse<PagedResponse<LeaveResponse>>> getLeavesByManager(
            @PathVariable Long managerId,
            @ParameterObject PaginationRequest paginationRequest) {
        PagedResponse<LeaveResponse> response = leaveService.getLeavesByManager(managerId, paginationRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Get leaves by status", description = "Get leave requests by status")
    public ResponseEntity<ApiResponse<PagedResponse<LeaveResponse>>> getLeavesByStatus(
            @PathVariable LeaveStatus status,
            @ParameterObject PaginationRequest paginationRequest) {
        PagedResponse<LeaveResponse> response = leaveService.getLeavesByStatus(status, paginationRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/manager/{managerId}/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Get pending leaves", description = "Get pending leave requests for manager")
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> getPendingLeavesForManager(@PathVariable Long managerId) {
        List<LeaveResponse> response = leaveService.getPendingLeavesForManager(managerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/balance/{employeeId}")
    @Operation(summary = "Get leave balance", description = "Get leave balance for employee")
    public ResponseEntity<ApiResponse<LeaveBalanceResponse>> getLeaveBalance(
            @PathVariable Long employeeId,
            @RequestParam Integer year) {
        LeaveBalanceResponse response = leaveService.getLeaveBalance(employeeId, year);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/balance/{employeeId}/initialize")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Initialize leave balance", description = "Initialize leave balance for employee")
    public ResponseEntity<ApiResponse<LeaveBalanceResponse>> initializeLeaveBalance(
            @PathVariable Long employeeId,
            @RequestParam Integer year) {
        LeaveBalanceResponse response = leaveService.initializeLeaveBalance(employeeId, year);
        return ResponseEntity.ok(ApiResponse.success("Leave balance initialized", response));
    }

    @PatchMapping("/balance/{employeeId}/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Adjust leave balance",
            description = "Signed delta on one bucket of an employee's balance. Required for comp-off credits and mistake corrections. Writes an audit log row.")
    public ResponseEntity<ApiResponse<LeaveBalanceResponse>> adjustLeaveBalance(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long employeeId,
            @Valid @RequestBody LeaveBalanceAdjustmentRequest request) {
        LeaveBalanceResponse response = leaveService.adjustBalance(employeeId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Leave balance adjusted", response));
    }

    @PostMapping("/{leaveRequestId}/override")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "HR / Admin override",
            description = "Forcibly flip an already-decided leave to APPROVED, REJECTED, or CANCELLED. "
                        + "Adjusts balance, writes an audit row, and notifies the applicant + all approvers.")
    public ResponseEntity<ApiResponse<LeaveResponse>> overrideLeave(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long leaveRequestId,
            @Valid @RequestBody LeaveOverrideRequest request) {
        LeaveResponse response = leaveService.overrideLeave(leaveRequestId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Leave override applied", response));
    }
}
