package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.dto.leave.LeaveApprovalRequest;
import com.financebuddha.finbud.hrms.dto.leave.LeaveBalanceResponse;
import com.financebuddha.finbud.hrms.dto.leave.LeaveRequestDTO;
import com.financebuddha.finbud.hrms.dto.leave.LeaveResponse;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.LeaveBalance;
import com.financebuddha.finbud.hrms.entity.LeaveRequest;
import com.financebuddha.finbud.hrms.enums.LeaveStatus;
import com.financebuddha.finbud.hrms.enums.LeaveType;
import com.financebuddha.finbud.hrms.exception.BadRequestException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.mapper.LeaveMapper;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.LeaveBalanceRepository;
import com.financebuddha.finbud.hrms.repository.LeaveRequestRepository;
import com.financebuddha.finbud.hrms.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveMapper leaveMapper;

    @Override
    @Transactional
    public LeaveResponse applyLeave(Long employeeId, LeaveRequestDTO request) {
        log.info("Applying leave for employee: {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        // Calculate days requested
        BigDecimal daysRequested;
        if (Boolean.TRUE.equals(request.getIsHalfDay())) {
            daysRequested = new BigDecimal("0.5");
        } else {
            daysRequested = BigDecimal.valueOf(ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1);
        }

        // Check balance
        if (request.getLeaveType() != LeaveType.LOP && !hasEnoughBalance(employeeId, request.getLeaveType(), daysRequested)) {
            throw new BadRequestException("Insufficient leave balance for " + request.getLeaveType());
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .leaveType(request.getLeaveType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .daysRequested(daysRequested)
                .reason(request.getReason())
                .contactDuringLeave(request.getContactDuringLeave())
                .isHalfDay(request.getIsHalfDay())
                .halfDayType(request.getHalfDayType())
                .status(LeaveStatus.PENDING)
                .build();

        if (employee.getManager() != null) {
            leaveRequest.setManager(employee.getManager());
        }

        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);
        return leaveMapper.toResponse(savedRequest);
    }

    @Override
    @Transactional
    public LeaveResponse approveLeave(Long leaveRequestId, Long approverId, LeaveApprovalRequest request) {
        log.info("Approving leave request: {}", leaveRequestId);

        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", leaveRequestId));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Leave request is not pending");
        }

        Employee approver = employeeRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", approverId));

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setApprovedBy(approver);
        leaveRequest.setApprovedAt(LocalDateTime.now());

        // Deduct leave balance
        deductLeaveBalance(leaveRequest.getEmployee().getId(), leaveRequest.getLeaveType(), leaveRequest.getDaysRequested());

        LeaveRequest updatedRequest = leaveRequestRepository.save(leaveRequest);
        return leaveMapper.toResponse(updatedRequest);
    }

    @Override
    @Transactional
    public LeaveResponse rejectLeave(Long leaveRequestId, Long approverId, String reason) {
        log.info("Rejecting leave request: {}", leaveRequestId);

        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", leaveRequestId));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Leave request is not pending");
        }

        Employee approver = employeeRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", approverId));

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setApprovedBy(approver);
        leaveRequest.setApprovedAt(LocalDateTime.now());
        leaveRequest.setRejectionReason(reason);

        LeaveRequest updatedRequest = leaveRequestRepository.save(leaveRequest);
        return leaveMapper.toResponse(updatedRequest);
    }

    @Override
    @Transactional
    public LeaveResponse cancelLeave(Long leaveRequestId, Long employeeId) {
        log.info("Cancelling leave request: {}", leaveRequestId);

        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", leaveRequestId));

        if (!leaveRequest.getEmployee().getId().equals(employeeId)) {
            throw new BadRequestException("Can only cancel your own leave requests");
        }

        if (leaveRequest.getStatus() == LeaveStatus.APPROVED) {
            // Restore leave balance
            restoreLeaveBalance(employeeId, leaveRequest.getLeaveType(), leaveRequest.getDaysRequested());
        }

        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        LeaveRequest updatedRequest = leaveRequestRepository.save(leaveRequest);
        return leaveMapper.toResponse(updatedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveResponse getLeaveById(Long id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));
        return leaveMapper.toResponse(leaveRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<LeaveResponse> getLeavesByEmployee(Long employeeId, PaginationRequest paginationRequest) {
        Pageable pageable = createPageable(paginationRequest);
        Page<LeaveRequest> leavePage = leaveRequestRepository.findByEmployeeId(employeeId, pageable);

        return PagedResponse.of(
                leaveMapper.toResponseList(leavePage.getContent()),
                leavePage.getNumber(),
                leavePage.getSize(),
                leavePage.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<LeaveResponse> getLeavesByManager(Long managerId, PaginationRequest paginationRequest) {
        Pageable pageable = createPageable(paginationRequest);
        Page<LeaveRequest> leavePage = leaveRequestRepository.findByManagerId(managerId, pageable);

        return PagedResponse.of(
                leaveMapper.toResponseList(leavePage.getContent()),
                leavePage.getNumber(),
                leavePage.getSize(),
                leavePage.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<LeaveResponse> getLeavesByStatus(LeaveStatus status, PaginationRequest paginationRequest) {
        Pageable pageable = createPageable(paginationRequest);
        Page<LeaveRequest> leavePage = leaveRequestRepository.findByStatus(status, pageable);

        return PagedResponse.of(
                leaveMapper.toResponseList(leavePage.getContent()),
                leavePage.getNumber(),
                leavePage.getSize(),
                leavePage.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponse> getPendingLeavesForManager(Long managerId) {
        List<LeaveRequest> leaves = leaveRequestRepository.findPendingLeavesForManager(managerId);
        return leaveMapper.toResponseList(leaves);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveBalanceResponse getLeaveBalance(Long employeeId, Integer year) {
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year)
                .orElseGet(() -> initializeLeaveBalanceEntity(employeeId, year));
        return leaveMapper.toBalanceResponse(balance);
    }

    @Override
    @Transactional
    public LeaveBalanceResponse initializeLeaveBalance(Long employeeId, Integer year) {
        LeaveBalance balance = initializeLeaveBalanceEntity(employeeId, year);
        return leaveMapper.toBalanceResponse(balance);
    }

    private LeaveBalance initializeLeaveBalanceEntity(Long employeeId, Integer year) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        LeaveBalance balance = LeaveBalance.builder()
                .employee(employee)
                .year(year)
                .build();

        return leaveBalanceRepository.save(balance);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasEnoughBalance(Long employeeId, LeaveType leaveType, BigDecimal days) {
        Integer year = LocalDateTime.now().getYear();
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year)
                .orElse(null);

        if (balance == null) return false;

        return switch (leaveType) {
            case CASUAL -> balance.getCasualLeaveBalance().compareTo(days) >= 0;
            case SICK -> balance.getSickLeaveBalance().compareTo(days) >= 0;
            case PAID -> balance.getPaidLeaveBalance().compareTo(days) >= 0;
            case WFH -> balance.getWfhBalance() >= days.intValue();
            case LOP -> true; // Loss of Pay doesn't require balance
        };
    }

    @Override
    @Transactional
    public void deductLeaveBalance(Long employeeId, LeaveType leaveType, BigDecimal days) {
        Integer year = LocalDateTime.now().getYear();
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveBalance", "employeeId", employeeId));

        switch (leaveType) {
            case CASUAL -> balance.setCasualLeaveUsed(balance.getCasualLeaveUsed().add(days));
            case SICK -> balance.setSickLeaveUsed(balance.getSickLeaveUsed().add(days));
            case PAID -> balance.setPaidLeaveUsed(balance.getPaidLeaveUsed().add(days));
            case WFH -> balance.setWfhDaysUsed(balance.getWfhDaysUsed() + days.intValue());
            case LOP -> balance.setLopDays(balance.getLopDays().add(days));
        }

        leaveBalanceRepository.save(balance);
    }

    @Override
    @Transactional
    public void restoreLeaveBalance(Long employeeId, LeaveType leaveType, BigDecimal days) {
        Integer year = LocalDateTime.now().getYear();
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveBalance", "employeeId", employeeId));

        switch (leaveType) {
            case CASUAL -> balance.setCasualLeaveUsed(balance.getCasualLeaveUsed().subtract(days).max(BigDecimal.ZERO));
            case SICK -> balance.setSickLeaveUsed(balance.getSickLeaveUsed().subtract(days).max(BigDecimal.ZERO));
            case PAID -> balance.setPaidLeaveUsed(balance.getPaidLeaveUsed().subtract(days).max(BigDecimal.ZERO));
            case WFH -> balance.setWfhDaysUsed(Math.max(0, balance.getWfhDaysUsed() - days.intValue()));
            case LOP -> balance.setLopDays(balance.getLopDays().subtract(days).max(BigDecimal.ZERO));
        }

        leaveBalanceRepository.save(balance);
    }

    private Pageable createPageable(PaginationRequest request) {
        Sort sort = Sort.by(request.getSortDirection().equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC,
                request.getSortBy());
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }
}
