package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.commitment.DailyCommitmentCreateRequest;
import com.financebuddha.finbud.hrms.dto.commitment.DailyCommitmentResponse;
import com.financebuddha.finbud.hrms.dto.commitment.DailyCommitmentReviewRequest;
import com.financebuddha.finbud.hrms.dto.commitment.DailyCommitmentUpdateRequest;
import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.exception.ForbiddenException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.repository.UserRepository;
import com.financebuddha.finbud.hrms.security.CurrentUser;
import com.financebuddha.finbud.hrms.security.UserPrincipal;
import com.financebuddha.finbud.hrms.service.DailyCommitmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Daily commitment endpoints — Q1 Phase A.
 *
 * <p>Authorisation:
 * <ul>
 *   <li>Employees write/read their own row (the service enforces the
 *       caller-id matches the row's employee_id).</li>
 *   <li>TL / HR / Admin can review SUBMITTED rows and read their team's
 *       rows. Method-level {@code @PreAuthorize} pins this down.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/commitments/daily")
@RequiredArgsConstructor
@Tag(name = "Daily Commitments", description = "Per-employee daily targets + actuals with TL approval")
@SecurityRequirement(name = "bearerAuth")
public class DailyCommitmentController {

    private final DailyCommitmentService dailyCommitmentService;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Create today's commitment", description = "Employee creates a DRAFT commitment with their day's targets.")
    public ResponseEntity<ApiResponse<DailyCommitmentResponse>> create(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody DailyCommitmentCreateRequest request) {
        DailyCommitmentResponse response = dailyCommitmentService.create(resolveEmployeeId(currentUser), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Commitment created", response));
    }

    @PutMapping("/{commitmentId}")
    @Operation(summary = "Patch a draft / rejected commitment",
               description = "Employee revises targets, fills actuals, or both. Only allowed in DRAFT or REJECTED.")
    public ResponseEntity<ApiResponse<DailyCommitmentResponse>> update(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long commitmentId,
            @Valid @RequestBody DailyCommitmentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                dailyCommitmentService.update(commitmentId, resolveEmployeeId(currentUser), request)));
    }

    @PostMapping("/{commitmentId}/submit")
    @Operation(summary = "Submit for TL approval",
               description = "DRAFT/REJECTED → SUBMITTED. Locks the row from further employee edits.")
    public ResponseEntity<ApiResponse<DailyCommitmentResponse>> submit(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long commitmentId) {
        return ResponseEntity.ok(ApiResponse.success("Commitment submitted",
                dailyCommitmentService.submit(commitmentId, resolveEmployeeId(currentUser))));
    }

    @PostMapping("/{commitmentId}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Approve or reject a submitted commitment",
               description = "TL/HR/Admin reviews. Rejection requires a reason so the employee can revise.")
    public ResponseEntity<ApiResponse<DailyCommitmentResponse>> review(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long commitmentId,
            @Valid @RequestBody DailyCommitmentReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Commitment reviewed",
                dailyCommitmentService.review(commitmentId, resolveEmployeeId(currentUser), request)));
    }

    @GetMapping("/{commitmentId}")
    @Operation(summary = "Get a commitment by id")
    public ResponseEntity<ApiResponse<DailyCommitmentResponse>> getById(@PathVariable Long commitmentId) {
        return ResponseEntity.ok(ApiResponse.success(dailyCommitmentService.getById(commitmentId)));
    }

    @GetMapping("/me")
    @Operation(summary = "My commitment for a specific date",
               description = "Returns 404 if no row exists for that date — the UI uses that to render an empty 'Create commitment' form.")
    public ResponseEntity<ApiResponse<DailyCommitmentResponse>> getMine(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                dailyCommitmentService.getByEmployeeAndDate(resolveEmployeeId(currentUser), date)));
    }

    @GetMapping("/me/range")
    @Operation(summary = "My commitment history within a date window")
    public ResponseEntity<ApiResponse<List<DailyCommitmentResponse>>> listMine(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(
                dailyCommitmentService.listForEmployee(resolveEmployeeId(currentUser), startDate, endDate)));
    }

    @GetMapping("/employee/{employeeId}/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Any employee's commitment history (TL/HR/Admin)")
    public ResponseEntity<ApiResponse<List<DailyCommitmentResponse>>> listForEmployee(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(
                dailyCommitmentService.listForEmployee(employeeId, startDate, endDate)));
    }

    @GetMapping("/manager/{managerId}/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Pending-approval queue for a manager")
    public ResponseEntity<ApiResponse<List<DailyCommitmentResponse>>> listPendingForManager(
            @PathVariable Long managerId) {
        return ResponseEntity.ok(ApiResponse.success(
                dailyCommitmentService.listPendingForManager(managerId)));
    }

    @GetMapping("/manager/{managerId}/team")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Team snapshot for a date")
    public ResponseEntity<ApiResponse<List<DailyCommitmentResponse>>> listTeamForDate(
            @PathVariable Long managerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                dailyCommitmentService.listTeamForDate(managerId, date)));
    }

    /**
     * {@link UserPrincipal#getId()} is the User row id; the service needs
     * the linked Employee row id. Same helper used by NotificationController
     * and AnnouncementController for consistency.
     */
    private Long resolveEmployeeId(UserPrincipal principal) {
        if (principal == null) {
            throw new ForbiddenException("Unauthenticated");
        }
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
        if (user.getEmployee() == null) {
            throw new ForbiddenException("Your login is not linked to an employee — ask HR to provision your profile.");
        }
        return user.getEmployee().getId();
    }
}
