package com.financebuddha.finbud.hrms.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.dto.leave.LeaveApprovalRequest;
import com.financebuddha.finbud.hrms.dto.leave.LeaveBalanceAdjustmentRequest;
import com.financebuddha.finbud.hrms.dto.leave.LeaveBalanceResponse;
import com.financebuddha.finbud.hrms.dto.leave.LeaveOverrideRequest;
import com.financebuddha.finbud.hrms.dto.leave.LeaveRequestDTO;
import com.financebuddha.finbud.hrms.dto.leave.LeaveResponse;
import com.financebuddha.finbud.hrms.entity.AuditLog;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.LeaveBalance;
import com.financebuddha.finbud.hrms.entity.LeaveRequest;
import com.financebuddha.finbud.hrms.enums.AuditAction;
import com.financebuddha.finbud.hrms.enums.LeaveStatus;
import com.financebuddha.finbud.hrms.enums.LeaveType;
import com.financebuddha.finbud.hrms.event.LeaveEvents;
import com.financebuddha.finbud.hrms.exception.BadRequestException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.mapper.LeaveMapper;
import com.financebuddha.finbud.hrms.repository.AuditLogRepository;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.LeaveBalanceRepository;
import com.financebuddha.finbud.hrms.repository.LeaveRequestRepository;
import com.financebuddha.finbud.hrms.security.AuthzService;
import com.financebuddha.finbud.hrms.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final AuditLogRepository auditLogRepository;
    private final LeaveMapper leaveMapper;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthzService authz;

    @Override
    @Transactional
    public LeaveResponse applyLeave(Long employeeId, LeaveRequestDTO request) {
        log.info("Applying leave for employee: {}", employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        // Ensure a leave-balance row exists for the current year BEFORE the
        // balance check below. The read path (getLeaveBalance) lazily creates
        // a missing row at the default 6+6 allocation via orElseGet, but the
        // apply path historically did not — so a brand-new employee whose
        // balance had never been viewed hit hasEnoughBalance()==false and got
        // a misleading "Insufficient leave balance" 400, making it look like
        // the form "never submits". Auto-initialise here to stay consistent.
        Integer currentYear = LocalDateTime.now().getYear();
        if (leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, currentYear).isEmpty()) {
            initializeLeaveBalanceEntity(employeeId, currentYear);
        }

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
        eventPublisher.publishEvent(new LeaveEvents.LeaveAppliedEvent(
                savedRequest.getId(), employee.getId()));
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
        eventPublisher.publishEvent(new LeaveEvents.LeaveApprovedEvent(
                updatedRequest.getId(),
                updatedRequest.getEmployee().getId(),
                approver.getId()));
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
        eventPublisher.publishEvent(new LeaveEvents.LeaveRejectedEvent(
                updatedRequest.getId(),
                updatedRequest.getEmployee().getId(),
                approver.getId(),
                reason));
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
        eventPublisher.publishEvent(new LeaveEvents.LeaveCancelledEvent(
                updatedRequest.getId(),
                updatedRequest.getEmployee().getId(),
                employeeId));
        return leaveMapper.toResponse(updatedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveResponse getLeaveById(Long id) {
        authz.requireOwnsLeaveOrPrivileged(id);
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", id));
        return leaveMapper.toResponse(leaveRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<LeaveResponse> getLeavesByEmployee(Long employeeId, PaginationRequest paginationRequest) {
        authz.requireOwnerOrPrivileged(employeeId);
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
    @Transactional // NOT readOnly: lazily inserts a default balance row on first read
    public LeaveBalanceResponse getLeaveBalance(Long employeeId, Integer year) {
        authz.requireOwnerOrPrivileged(employeeId);
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

        // CASUAL and SICK share a single pool per Finbud policy (V12).
        return switch (leaveType) {
            case CASUAL, SICK -> balance.getCasualSickBalance().compareTo(days) >= 0;
            case PAID -> balance.getPaidLeaveBalance().compareTo(days) >= 0;
            case LOP -> true; // Loss of Pay doesn't require balance
        };
    }

    @Override
    @Transactional
    public void deductLeaveBalance(Long employeeId, LeaveType leaveType, BigDecimal days) {
        Integer year = LocalDateTime.now().getYear();
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveBalance", "employeeId", employeeId));

        // CASUAL + SICK deduct from the shared pool.
        switch (leaveType) {
            case CASUAL, SICK -> balance.setCasualSickUsed(balance.getCasualSickUsed().add(days));
            case PAID -> balance.setPaidLeaveUsed(balance.getPaidLeaveUsed().add(days));
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

        // Restore into the same bucket the deduction came from. Clamp to
        // zero so a buggy double-restore can't produce a negative "used".
        switch (leaveType) {
            case CASUAL, SICK -> balance.setCasualSickUsed(balance.getCasualSickUsed().subtract(days).max(BigDecimal.ZERO));
            case PAID -> balance.setPaidLeaveUsed(balance.getPaidLeaveUsed().subtract(days).max(BigDecimal.ZERO));
            case LOP -> balance.setLopDays(balance.getLopDays().subtract(days).max(BigDecimal.ZERO));
        }

        leaveBalanceRepository.save(balance);
    }

    @Override
    @Transactional
    public LeaveBalanceResponse adjustBalance(Long employeeId, Long adjusterId, LeaveBalanceAdjustmentRequest request) {
        log.info("HR balance adjust: employee={}, adjuster={}, bucket={}, delta={}",
                employeeId, adjusterId, request.getBucket(), request.getDelta());

        if (request.getDelta() == null || request.getDelta().signum() == 0) {
            throw new BadRequestException("Delta must be non-zero");
        }

        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, request.getYear())
                .orElseThrow(() -> new ResourceNotFoundException("LeaveBalance", "employeeId", employeeId));

        Employee adjuster = employeeRepository.findById(adjusterId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", adjusterId));

        // Capture "before" for the audit row. We store just the impacted
        // bucket plus year/employee — no need to dump the whole entity.
        String oldValues = serialiseBalanceSnapshot(balance);

        BigDecimal delta = request.getDelta();
        switch (request.getBucket()) {
            case CASUAL_SICK -> balance.setCasualSickAllocated(
                    balance.getCasualSickAllocated().add(delta).max(BigDecimal.ZERO));
            case PAID -> balance.setPaidLeaveAllocated(
                    balance.getPaidLeaveAllocated().add(delta).max(BigDecimal.ZERO));
            case LOP -> balance.setLopDays(
                    balance.getLopDays().add(delta).max(BigDecimal.ZERO));
        }

        LeaveBalance saved = leaveBalanceRepository.save(balance);

        // Audit-log the mutation. We piggyback on the generic `audit_logs`
        // table so the Audit Logs viewer (T2-4) surfaces leave adjustments
        // alongside every other administrative action.
        AuditLog auditEntry = AuditLog.builder()
                .tableName("leave_balances")
                .recordId(saved.getId())
                .action(AuditAction.UPDATE)
                .oldValues(oldValues)
                .newValues(serialiseBalanceSnapshot(saved))
                .performedBy(adjuster)
                .reason(request.getReason())
                .build();
        auditLogRepository.save(auditEntry);

        return leaveMapper.toBalanceResponse(saved);
    }

    @Override
    @Transactional
    public LeaveResponse overrideLeave(Long leaveRequestId, Long overriderId, LeaveOverrideRequest request) {
        log.info("HR override: leave={}, overrider={}, target={}",
                leaveRequestId, overriderId, request.getTargetStatus());

        // Reject illegal target states up front. Override doesn't bring a
        // request back into PENDING (no business case) and WITHDRAWN is
        // employee-driven, not HR-driven.
        LeaveStatus target = request.getTargetStatus();
        if (target != LeaveStatus.APPROVED
                && target != LeaveStatus.REJECTED
                && target != LeaveStatus.CANCELLED) {
            throw new BadRequestException("Override target must be APPROVED, REJECTED, or CANCELLED");
        }

        LeaveRequest leave = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", "id", leaveRequestId));

        Employee overrider = employeeRepository.findById(overriderId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", overriderId));

        LeaveStatus oldStatus = leave.getStatus();
        if (oldStatus == target) {
            // No-op override — return the response without churning the
            // balance or writing an audit row. Useful for idempotency.
            return leaveMapper.toResponse(leave);
        }

        // Balance arithmetic — figure out what happens to the bucket.
        // Using a small decision table:
        //   old=APPROVED, new=REJECTED   → restore
        //   old=APPROVED, new=CANCELLED  → restore
        //   old=REJECTED, new=APPROVED   → re-deduct (must check balance)
        //   old=CANCELLED, new=APPROVED  → re-deduct (must check balance)
        //   old=PENDING,  new=APPROVED   → deduct
        //   old=PENDING,  new=REJECTED   → no-op (never deducted)
        //   old=PENDING,  new=CANCELLED  → no-op
        //   old=REJECTED, new=CANCELLED  → no-op
        //   old=CANCELLED,new=REJECTED   → no-op
        boolean wasCommitted = oldStatus == LeaveStatus.APPROVED;
        boolean willCommit   = target == LeaveStatus.APPROVED;
        Long applicantId = leave.getEmployee().getId();
        BigDecimal days = leave.getDaysRequested();

        if (wasCommitted && !willCommit) {
            restoreLeaveBalance(applicantId, leave.getLeaveType(), days);
        } else if (!wasCommitted && willCommit) {
            // Re-deducting — confirm balance is sufficient. LOP is always
            // allowed; the rest must check.
            if (leave.getLeaveType() != LeaveType.LOP
                    && !hasEnoughBalance(applicantId, leave.getLeaveType(), days)) {
                throw new BadRequestException(
                        "Cannot override to APPROVED — insufficient leave balance for " + leave.getLeaveType());
            }
            deductLeaveBalance(applicantId, leave.getLeaveType(), days);
        }

        // Persist the new state. We use approvedBy/approvedAt to capture
        // the most recent decision-maker, regardless of whether they're
        // approving or rejecting (the timestamp is "decision time", which
        // matches the existing approve / reject paths).
        leave.setStatus(target);
        leave.setApprovedBy(overrider);
        leave.setApprovedAt(LocalDateTime.now());
        if (target == LeaveStatus.REJECTED) {
            leave.setRejectionReason(request.getReason());
        }
        LeaveRequest saved = leaveRequestRepository.save(leave);

        // Audit row — old/new statuses + the HR's reason. Goes into
        // the same `audit_logs` table that the Audit Logs viewer (T2-4)
        // will read from.
        AuditLog audit = AuditLog.builder()
                .tableName("leave_requests")
                .recordId(saved.getId())
                .action(AuditAction.OVERRIDE)
                .oldValues(serialiseStatusSnapshot(oldStatus))
                .newValues(serialiseStatusSnapshot(target))
                .performedBy(overrider)
                .reason(request.getReason())
                .build();
        auditLogRepository.save(audit);

        eventPublisher.publishEvent(new LeaveEvents.LeaveOverriddenEvent(
                saved.getId(),
                applicantId,
                overrider.getId(),
                oldStatus,
                target,
                request.getReason()));

        return leaveMapper.toResponse(saved);
    }

    /** Tiny helper for the override audit row — old/new is just a status. */
    private String serialiseStatusSnapshot(LeaveStatus status) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of("status", status.name()));
        } catch (JsonProcessingException ex) {
            log.warn("Could not serialise status {} for audit: {}", status, ex.getMessage());
            return null;
        }
    }

    /**
     * Serialise a {@link LeaveBalance} to a compact JSON snapshot suitable
     * for the {@code audit_logs.old_values} / {@code new_values} jsonb
     * columns. Catches Jackson errors and logs them — we don't want an
     * audit-write failure to rollback the actual balance change.
     */
    private String serialiseBalanceSnapshot(LeaveBalance balance) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of(
                    "year", balance.getYear(),
                    "casualSickAllocated", balance.getCasualSickAllocated(),
                    "casualSickUsed", balance.getCasualSickUsed(),
                    "paidLeaveAllocated", balance.getPaidLeaveAllocated(),
                    "paidLeaveUsed", balance.getPaidLeaveUsed(),
                    "lopDays", balance.getLopDays()
            ));
        } catch (JsonProcessingException ex) {
            log.warn("Could not serialise LeaveBalance id={} for audit: {}", balance.getId(), ex.getMessage());
            return null;
        }
    }

    private Pageable createPageable(PaginationRequest request) {
        Sort sort = Sort.by(request.getSortDirection().equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC,
                request.getSortBy());
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }
}
