package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.dto.leave.LeaveApprovalRequest;
import com.financebuddha.finbud.hrms.dto.leave.LeaveBalanceAdjustmentRequest;
import com.financebuddha.finbud.hrms.dto.leave.LeaveBalanceResponse;
import com.financebuddha.finbud.hrms.dto.leave.LeaveOverrideRequest;
import com.financebuddha.finbud.hrms.dto.leave.LeaveRequestDTO;
import com.financebuddha.finbud.hrms.dto.leave.LeaveResponse;
import com.financebuddha.finbud.hrms.enums.LeaveStatus;
import com.financebuddha.finbud.hrms.enums.LeaveType;

import java.math.BigDecimal;
import java.util.List;

public interface LeaveService {

    LeaveResponse applyLeave(Long employeeId, LeaveRequestDTO request);

    LeaveResponse approveLeave(Long leaveRequestId, Long approverId, LeaveApprovalRequest request);

    LeaveResponse rejectLeave(Long leaveRequestId, Long approverId, String reason);

    LeaveResponse cancelLeave(Long leaveRequestId, Long employeeId);

    LeaveResponse getLeaveById(Long id);

    PagedResponse<LeaveResponse> getLeavesByEmployee(Long employeeId, PaginationRequest paginationRequest);

    PagedResponse<LeaveResponse> getLeavesByManager(Long managerId, PaginationRequest paginationRequest);

    PagedResponse<LeaveResponse> getLeavesByStatus(LeaveStatus status, PaginationRequest paginationRequest);

    List<LeaveResponse> getPendingLeavesForManager(Long managerId);

    LeaveBalanceResponse getLeaveBalance(Long employeeId, Integer year);

    LeaveBalanceResponse initializeLeaveBalance(Long employeeId, Integer year);

    boolean hasEnoughBalance(Long employeeId, LeaveType leaveType, BigDecimal days);

    void deductLeaveBalance(Long employeeId, LeaveType leaveType, BigDecimal days);

    void restoreLeaveBalance(Long employeeId, LeaveType leaveType, BigDecimal days);

    /**
     * HR-only manual adjustment of an employee's balance (comp-off, mistake
     * correction, mid-year policy change). Persists to {@code audit_logs}
     * so the change is traceable.
     *
     * @param employeeId whose balance to adjust
     * @param adjusterId acting HR/Admin employee id (for the audit log)
     * @param request bucket + signed delta + mandatory reason
     */
    LeaveBalanceResponse adjustBalance(Long employeeId, Long adjusterId, LeaveBalanceAdjustmentRequest request);

    /**
     * HR / Admin override of an already-decided leave. Flips the status to
     * {@code targetStatus} regardless of the previous state, applying the
     * matching balance adjustment (deduct/restore) and writing an audit row.
     * Publishes a {@code LeaveOverriddenEvent} that fans out notifications
     * to the applicant and all approvers.
     *
     * <p>Authorisation enforcement is at the controller layer
     * ({@code @PreAuthorize("hasAnyRole('ADMIN', 'HR')")}).
     *
     * @param leaveRequestId the leave to override
     * @param overriderId    employee id of the acting HR / Admin (for audit)
     * @param request        target status + mandatory reason
     */
    LeaveResponse overrideLeave(Long leaveRequestId, Long overriderId, LeaveOverrideRequest request);
}
