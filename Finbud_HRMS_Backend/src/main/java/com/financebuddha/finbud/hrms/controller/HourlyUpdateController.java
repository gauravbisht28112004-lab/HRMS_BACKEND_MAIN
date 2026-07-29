package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.commitment.HourlyUpdateRequest;
import com.financebuddha.finbud.hrms.dto.commitment.HourlyUpdateResponse;
import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.exception.ForbiddenException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.repository.UserRepository;
import com.financebuddha.finbud.hrms.security.CurrentUser;
import com.financebuddha.finbud.hrms.security.UserPrincipal;
import com.financebuddha.finbud.hrms.service.HourlyUpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Hourly updates — Q1 Phase B. Pure self-tracking; no approval flow.
 */
@RestController
@RequestMapping("/api/commitments/hourly")
@RequiredArgsConstructor
@Tag(name = "Hourly Updates", description = "Per-hour activity log feeding daily/weekly reports")
@SecurityRequirement(name = "bearerAuth")
public class HourlyUpdateController {

    private final HourlyUpdateService hourlyUpdateService;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Submit / update an hourly entry",
               description = "Upsert: same (date, slot) updates in place. No approval workflow.")
    public ResponseEntity<ApiResponse<HourlyUpdateResponse>> upsert(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody HourlyUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Hourly update saved",
                hourlyUpdateService.upsert(resolveEmployeeId(currentUser), request)));
    }

    @DeleteMapping("/{updateId}")
    @Operation(summary = "Delete one of my hourly updates")
    public ResponseEntity<ApiResponse<Void>> delete(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long updateId) {
        hourlyUpdateService.delete(updateId, resolveEmployeeId(currentUser));
        return ResponseEntity.ok(ApiResponse.success("Hourly update deleted", null));
    }

    @GetMapping("/me")
    @Operation(summary = "My hourly entries for a single date")
    public ResponseEntity<ApiResponse<List<HourlyUpdateResponse>>> listMine(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                hourlyUpdateService.listMineForDate(resolveEmployeeId(currentUser), date)));
    }

    @GetMapping("/me/range")
    @Operation(summary = "My hourly entries within a date window")
    public ResponseEntity<ApiResponse<List<HourlyUpdateResponse>>> listMineForRange(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(
                hourlyUpdateService.listMineForRange(resolveEmployeeId(currentUser), startDate, endDate)));
    }

    @GetMapping("/manager/{managerId}/team")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Team snapshot for a date")
    public ResponseEntity<ApiResponse<List<HourlyUpdateResponse>>> listTeamForDate(
            @PathVariable Long managerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                hourlyUpdateService.listTeamForDate(managerId, date)));
    }

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
