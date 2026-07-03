package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.atl.AtlDashboardResponse;
import com.financebuddha.finbud.hrms.dto.atl.AtlSummaryEntryResponse;
import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.exception.ForbiddenException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.repository.UserRepository;
import com.financebuddha.finbud.hrms.security.CurrentUser;
import com.financebuddha.finbud.hrms.security.UserPrincipal;
import com.financebuddha.finbud.hrms.service.AtlDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * ATL (Assistant Team Leader) dashboard endpoints.
 *
 * <p>Flow this supports:
 * <ol>
 *   <li>HR/Admin grants ROLE_ATL to an employee via the existing
 *       {@code PUT /api/admin/users/{userId}/roles} endpoint.</li>
 *   <li>HR/Admin assigns employees "under" that ATL via the existing
 *       {@code PUT /api/employees/{id}} endpoint, setting {@code managerId}
 *       to the ATL's employee id — the same mechanic already used for
 *       MANAGER teams.</li>
 *   <li>The ATL views {@code /me/team-commitment} for their own cumulative
 *       committed disbursal; HR/Admin view {@code /summary} for the
 *       cumulative figure per ATL, or {@code /{atlId}/team-commitment} for
 *       one ATL's breakdown.</li>
 * </ol>
 *
 * <p>Authorisation mirrors the existing MANAGER endpoints in
 * {@code DailyCommitmentController}: role-gated, not further restricted to
 * "your own team only" for ADMIN/HR/ATL callers — consistent with how
 * {@code /api/commitments/daily/manager/{managerId}/team} already works.
 */
@RestController
@RequestMapping("/api/atl")
@RequiredArgsConstructor
@Tag(name = "ATL Dashboard", description = "Assistant Team Leader cumulative commitment dashboards")
@SecurityRequirement(name = "bearerAuth")
public class AtlDashboardController {

    private final AtlDashboardService atlDashboardService;
    private final UserRepository userRepository;

    @GetMapping("/me/team-commitment")
    @Operation(summary = "My team's cumulative commitment",
               description = "For the logged-in ATL: cumulative committed (target) disbursal of employees assigned under them, for a date range.")
    public ResponseEntity<ApiResponse<AtlDashboardResponse>> getMyTeamDashboard(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(
                atlDashboardService.getTeamDashboard(resolveEmployeeId(currentUser), startDate, endDate)));
    }

    @GetMapping("/{atlId}/team-commitment")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'ATL')")
    @Operation(summary = "One ATL's cumulative commitment (HR/Admin, or any ATL)",
               description = "Cumulative committed (target) disbursal of employees assigned under this ATL, plus a per-employee breakdown.")
    public ResponseEntity<ApiResponse<AtlDashboardResponse>> getTeamDashboard(
            @PathVariable Long atlId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(
                atlDashboardService.getTeamDashboard(atlId, startDate, endDate)));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "HR/Admin summary across all ATLs",
               description = "One row per ATL: team size and cumulative committed (target) disbursal for the date range, sorted highest-first.")
    public ResponseEntity<ApiResponse<List<AtlSummaryEntryResponse>>> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.success(
                atlDashboardService.getAtlSummary(startDate, endDate)));
    }

    /**
     * {@link UserPrincipal#getId()} is the User row id; the service needs
     * the linked Employee row id. Same helper used by DailyCommitmentController
     * and MonthlyTargetController for consistency.
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
