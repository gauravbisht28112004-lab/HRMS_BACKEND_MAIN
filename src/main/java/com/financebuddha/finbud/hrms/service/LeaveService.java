package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.dto.leave.LeaveApprovalRequest;
import com.financebuddha.finbud.hrms.dto.leave.LeaveBalanceResponse;
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
}
