package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.commitment.MonthlyTargetResponse;
import com.financebuddha.finbud.hrms.dto.commitment.MonthlyTargetUpsertRequest;
import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.exception.ForbiddenException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.repository.UserRepository;
import com.financebuddha.finbud.hrms.security.CurrentUser;
import com.financebuddha.finbud.hrms.security.UserPrincipal;
import com.financebuddha.finbud.hrms.service.MonthlyTargetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Monthly target endpoints — Q1 Phase C.
 *
 * <p>Authorisation:
 * <ul>
 *   <li>Set / upsert: TL/HR/Admin only.</li>
 *   <li>Read-own ({@code /me}): any authenticated user — employees see
 *       their own target + achieved overlay.</li>
 *   <li>Team list: TL/HR/Admin.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/commitments/monthly-target")
@RequiredArgsConstructor
@Tag(name = "Monthly Targets", description = "Per-employee per-month sales target with achieved overlay")
@SecurityRequirement(name = "bearerAuth")
public class MonthlyTargetController {

    private final MonthlyTargetService monthlyTargetService;
    private final UserRepository userRepository;

    @PutMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Set / update an employee's monthly target",
               description = "TL/HR/Admin only. Same (employee, year, month) overwrites in place.")
    public ResponseEntity<ApiResponse<MonthlyTargetResponse>> upsert(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long employeeId,
            @Valid @RequestBody MonthlyTargetUpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Monthly target saved",
                monthlyTargetService.upsert(employeeId, resolveEmployeeId(currentUser), request)));
    }

    @GetMapping("/me")
    @Operation(summary = "My monthly target + achieved overlay",
               description = "Returns a zero-target placeholder if no target has been set yet.")
    public ResponseEntity<ApiResponse<MonthlyTargetResponse>> getMine(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return ResponseEntity.ok(ApiResponse.success(
                monthlyTargetService.get(resolveEmployeeId(currentUser), year, month)));
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Any employee's monthly target", description = "TL/HR/Admin only.")
    public ResponseEntity<ApiResponse<MonthlyTargetResponse>> getOne(
            @PathVariable Long employeeId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return ResponseEntity.ok(ApiResponse.success(monthlyTargetService.get(employeeId, year, month)));
    }

    @GetMapping("/manager/{managerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Team list", description = "Direct reports' monthly targets with achieved overlay.")
    public ResponseEntity<ApiResponse<List<MonthlyTargetResponse>>> listForManager(
            @PathVariable Long managerId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return ResponseEntity.ok(ApiResponse.success(
                monthlyTargetService.listForManager(managerId, year, month)));
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
