package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.attendance.RegularizationRequestDto;
import com.financebuddha.finbud.hrms.dto.attendance.RegularizationResponse;
import com.financebuddha.finbud.hrms.dto.attendance.RegularizationReviewRequest;
import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.security.CurrentUser;
import com.financebuddha.finbud.hrms.security.UserPrincipal;
import com.financebuddha.finbud.hrms.service.RegularizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/regularizations")
@RequiredArgsConstructor
@Tag(name = "Regularizations", description = "Attendance regularization requests and reviews")
@SecurityRequirement(name = "bearerAuth")
public class RegularizationController {

    private final RegularizationService regularizationService;

    @PostMapping
    @Operation(summary = "Submit regularization request",
            description = "Employee files a correction for one of their own attendance days")
    public ResponseEntity<ApiResponse<RegularizationResponse>> submit(
            @Valid @RequestBody RegularizationRequestDto request,
            @CurrentUser UserPrincipal principal) {
        RegularizationResponse response = regularizationService.submit(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Regularization request submitted", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel own regularization", description = "Employee withdraws their own pending request")
    public ResponseEntity<ApiResponse<Void>> cancelOwn(
            @PathVariable Long id,
            @CurrentUser UserPrincipal principal) {
        regularizationService.cancelOwnRequest(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Regularization cancelled", null));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Review regularization",
            description = "Manager (for direct reports), HR, or Admin approves or rejects the request")
    public ResponseEntity<ApiResponse<RegularizationResponse>> review(
            @PathVariable Long id,
            @Valid @RequestBody RegularizationReviewRequest request,
            @CurrentUser UserPrincipal principal) {
        RegularizationResponse response = regularizationService.review(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Regularization reviewed", response));
    }

    @GetMapping("/me")
    @Operation(summary = "My regularization requests", description = "History for the currently authenticated employee")
    public ResponseEntity<ApiResponse<List<RegularizationResponse>>> listMyRequests(
            @CurrentUser UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                regularizationService.listMyRequests(principal)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Pending regularization queue",
            description = "HR/Admin see all; Manager sees only direct reports")
    public ResponseEntity<ApiResponse<List<RegularizationResponse>>> listPending(
            @CurrentUser UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                regularizationService.listPendingForApprover(principal)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get regularization by id")
    public ResponseEntity<ApiResponse<RegularizationResponse>> getById(
            @PathVariable Long id,
            @CurrentUser UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(
                regularizationService.getById(id, principal)));
    }
}
