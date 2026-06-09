package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.commitment.DailyCommitmentCreateRequest;
import com.financebuddha.finbud.hrms.dto.commitment.DailyCommitmentResponse;
import com.financebuddha.finbud.hrms.dto.commitment.DailyCommitmentReviewRequest;
import com.financebuddha.finbud.hrms.dto.commitment.DailyCommitmentUpdateRequest;

import java.time.LocalDate;
import java.util.List;

public interface DailyCommitmentService {

    /** Employee creates today's (or any) commitment. Status starts DRAFT. */
    DailyCommitmentResponse create(Long employeeId, DailyCommitmentCreateRequest request);

    /** Patch targets / actuals / notes. Only allowed while DRAFT or REJECTED. */
    DailyCommitmentResponse update(Long commitmentId, Long callerEmployeeId, DailyCommitmentUpdateRequest request);

    /** Employee submits the day's commitment for TL approval. DRAFT/REJECTED → SUBMITTED. */
    DailyCommitmentResponse submit(Long commitmentId, Long callerEmployeeId);

    /** TL / HR / Admin approves or rejects a SUBMITTED row. */
    DailyCommitmentResponse review(Long commitmentId, Long reviewerEmployeeId, DailyCommitmentReviewRequest request);

    /** Single row by id. */
    DailyCommitmentResponse getById(Long commitmentId);

    /** Employee's own row for a specific date (404 if missing). */
    DailyCommitmentResponse getByEmployeeAndDate(Long employeeId, LocalDate workDate);

    /** Employee's history within a date window, newest first. */
    List<DailyCommitmentResponse> listForEmployee(Long employeeId, LocalDate startDate, LocalDate endDate);

    /** Pending-approval queue for a manager (their direct reports' SUBMITTED rows). */
    List<DailyCommitmentResponse> listPendingForManager(Long managerId);

    /** Manager's team snapshot for a single date. */
    List<DailyCommitmentResponse> listTeamForDate(Long managerId, LocalDate workDate);
}
