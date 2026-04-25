package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.commitment.DailyCommitmentCreateRequest;
import com.financebuddha.finbud.hrms.dto.commitment.DailyCommitmentResponse;
import com.financebuddha.finbud.hrms.dto.commitment.DailyCommitmentReviewRequest;
import com.financebuddha.finbud.hrms.dto.commitment.DailyCommitmentUpdateRequest;
import com.financebuddha.finbud.hrms.entity.DailyCommitment;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.enums.CommitmentStatus;
import com.financebuddha.finbud.hrms.exception.BadRequestException;
import com.financebuddha.finbud.hrms.exception.ForbiddenException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.repository.DailyCommitmentRepository;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.service.DailyCommitmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyCommitmentServiceImpl implements DailyCommitmentService {

    private final DailyCommitmentRepository dailyCommitmentRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional
    public DailyCommitmentResponse create(Long employeeId, DailyCommitmentCreateRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        // The DB has a UNIQUE (employee_id, work_date) constraint — pre-empt
        // it with a friendly message so the UI doesn't see a generic 409.
        dailyCommitmentRepository.findByEmployeeIdAndWorkDate(employeeId, request.getWorkDate())
                .ifPresent(existing -> {
                    throw new BadRequestException(
                            "A commitment already exists for " + request.getWorkDate()
                                    + ". Use the update endpoint to revise it.");
                });

        DailyCommitment commitment = DailyCommitment.builder()
                .employee(employee)
                .workDate(request.getWorkDate())
                .targetCalls(request.getTargetCalls())
                .targetOtps(request.getTargetOtps())
                .targetInterestedCustomers(request.getTargetInterestedCustomers())
                .targetDisbursalAmount(request.getTargetDisbursalAmount())
                .notes(request.getNotes())
                .status(CommitmentStatus.DRAFT)
                .build();

        return toResponse(dailyCommitmentRepository.save(commitment));
    }

    @Override
    @Transactional
    public DailyCommitmentResponse update(Long commitmentId, Long callerEmployeeId, DailyCommitmentUpdateRequest request) {
        DailyCommitment commitment = loadOrThrow(commitmentId);

        // Ownership: only the employee who owns the row can edit it.
        if (!commitment.getEmployee().getId().equals(callerEmployeeId)) {
            throw new ForbiddenException("You can only edit your own commitments");
        }

        // Only DRAFT and REJECTED rows are editable. SUBMITTED is locked
        // to the TL; APPROVED is locked to HR override (separate endpoint).
        if (commitment.getStatus() != CommitmentStatus.DRAFT
                && commitment.getStatus() != CommitmentStatus.REJECTED) {
            throw new BadRequestException(
                    "Cannot edit a commitment in status " + commitment.getStatus()
                            + " — submit a new revision instead.");
        }

        // Patch only the fields the caller actually sent. Nulls = leave as-is.
        if (request.getTargetCalls() != null) commitment.setTargetCalls(request.getTargetCalls());
        if (request.getTargetOtps() != null) commitment.setTargetOtps(request.getTargetOtps());
        if (request.getTargetInterestedCustomers() != null)
            commitment.setTargetInterestedCustomers(request.getTargetInterestedCustomers());
        if (request.getTargetDisbursalAmount() != null)
            commitment.setTargetDisbursalAmount(request.getTargetDisbursalAmount());
        if (request.getActualCalls() != null) commitment.setActualCalls(request.getActualCalls());
        if (request.getActualOtps() != null) commitment.setActualOtps(request.getActualOtps());
        if (request.getActualInterestedCustomers() != null)
            commitment.setActualInterestedCustomers(request.getActualInterestedCustomers());
        if (request.getActualDisbursalAmount() != null)
            commitment.setActualDisbursalAmount(request.getActualDisbursalAmount());
        if (request.getNotes() != null) commitment.setNotes(request.getNotes());

        return toResponse(dailyCommitmentRepository.save(commitment));
    }

    @Override
    @Transactional
    public DailyCommitmentResponse submit(Long commitmentId, Long callerEmployeeId) {
        DailyCommitment commitment = loadOrThrow(commitmentId);

        if (!commitment.getEmployee().getId().equals(callerEmployeeId)) {
            throw new ForbiddenException("You can only submit your own commitments");
        }
        if (commitment.getStatus() != CommitmentStatus.DRAFT
                && commitment.getStatus() != CommitmentStatus.REJECTED) {
            throw new BadRequestException(
                    "Only DRAFT or REJECTED commitments can be submitted; current status is "
                            + commitment.getStatus());
        }

        commitment.setStatus(CommitmentStatus.SUBMITTED);
        commitment.setSubmittedAt(LocalDateTime.now());
        // Clear any prior rejection so the TL sees a clean slate on this revision.
        commitment.setRejectionReason(null);
        return toResponse(dailyCommitmentRepository.save(commitment));
    }

    @Override
    @Transactional
    public DailyCommitmentResponse review(Long commitmentId, Long reviewerEmployeeId, DailyCommitmentReviewRequest request) {
        DailyCommitment commitment = loadOrThrow(commitmentId);

        if (commitment.getStatus() != CommitmentStatus.SUBMITTED) {
            throw new BadRequestException(
                    "Only SUBMITTED commitments can be reviewed; current status is "
                            + commitment.getStatus());
        }

        Employee reviewer = employeeRepository.findById(reviewerEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", reviewerEmployeeId));

        if (Boolean.TRUE.equals(request.getApprove())) {
            commitment.setStatus(CommitmentStatus.APPROVED);
            commitment.setRejectionReason(null);
        } else {
            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                throw new BadRequestException("Rejection reason is required when rejecting a commitment");
            }
            commitment.setStatus(CommitmentStatus.REJECTED);
            commitment.setRejectionReason(request.getRejectionReason().trim());
        }

        commitment.setApprovedBy(reviewer);
        commitment.setApprovedAt(LocalDateTime.now());

        log.info("Commitment #{} reviewed by employee {} → {}",
                commitmentId, reviewerEmployeeId, commitment.getStatus());

        return toResponse(dailyCommitmentRepository.save(commitment));
    }

    @Override
    @Transactional(readOnly = true)
    public DailyCommitmentResponse getById(Long commitmentId) {
        return toResponse(loadOrThrow(commitmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public DailyCommitmentResponse getByEmployeeAndDate(Long employeeId, LocalDate workDate) {
        DailyCommitment commitment = dailyCommitmentRepository
                .findByEmployeeIdAndWorkDate(employeeId, workDate)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DailyCommitment", "employeeId+workDate", employeeId + "/" + workDate));
        return toResponse(commitment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyCommitmentResponse> listForEmployee(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return dailyCommitmentRepository
                .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDesc(employeeId, startDate, endDate)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyCommitmentResponse> listPendingForManager(Long managerId) {
        return dailyCommitmentRepository
                .findByManagerIdAndStatus(managerId, CommitmentStatus.SUBMITTED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyCommitmentResponse> listTeamForDate(Long managerId, LocalDate workDate) {
        return dailyCommitmentRepository
                .findByManagerIdAndWorkDate(managerId, workDate)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private DailyCommitment loadOrThrow(Long commitmentId) {
        return dailyCommitmentRepository.findById(commitmentId)
                .orElseThrow(() -> new ResourceNotFoundException("DailyCommitment", "id", commitmentId));
    }

    private DailyCommitmentResponse toResponse(DailyCommitment c) {
        Employee employee = c.getEmployee();
        Employee approver = c.getApprovedBy();
        return DailyCommitmentResponse.builder()
                .id(c.getId())
                .employeeId(employee != null ? employee.getId() : null)
                .employeeCode(employee != null ? employee.getEmployeeId() : null)
                .employeeName(employee != null ? employee.getFullName() : null)
                .workDate(c.getWorkDate())
                .targetCalls(c.getTargetCalls())
                .targetOtps(c.getTargetOtps())
                .targetInterestedCustomers(c.getTargetInterestedCustomers())
                .targetDisbursalAmount(c.getTargetDisbursalAmount())
                .actualCalls(c.getActualCalls())
                .actualOtps(c.getActualOtps())
                .actualInterestedCustomers(c.getActualInterestedCustomers())
                .actualDisbursalAmount(c.getActualDisbursalAmount())
                .status(c.getStatus())
                .submittedAt(c.getSubmittedAt())
                .approvedById(approver != null ? approver.getId() : null)
                .approvedByName(approver != null ? approver.getFullName() : null)
                .approvedAt(c.getApprovedAt())
                .rejectionReason(c.getRejectionReason())
                .notes(c.getNotes())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
