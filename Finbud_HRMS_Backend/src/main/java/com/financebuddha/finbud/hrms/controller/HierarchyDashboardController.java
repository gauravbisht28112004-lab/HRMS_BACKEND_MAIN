package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.dto.hierarchy.HierarchyDashboardResponse;
import com.financebuddha.finbud.hrms.dto.hierarchy.HierarchyReportRow;
import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.enums.RoleType;
import com.financebuddha.finbud.hrms.exception.ForbiddenException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.repository.UserRepository;
import com.financebuddha.finbud.hrms.security.CurrentUser;
import com.financebuddha.finbud.hrms.security.UserPrincipal;
import com.financebuddha.finbud.hrms.service.HierarchyDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Target-flow dashboards for the reporting chain:
 *
 * <pre>
 *   Admin  ->  Manager  ->  Team Leader  ->  ATL  ->  Employee
 * </pre>
 *
 * <p>Each signed-in user reads their OWN dashboard via {@code /dashboard/me}:
 * their monthly target (handed down by the level above), their whole team's
 * disbursal to date, and a breakdown of their DIRECT reports only. No endpoint
 * lets a level peek more than one step below itself — enforcing the
 * "no overlapping data" rule between levels.
 *
 * <p>Admin/HR additionally get the flat {@code /all-employees} option, listing
 * every active employee's target and own disbursal — separate from the
 * "managers under me" view they get from {@code /dashboard/me}.
 */
@RestController
@RequestMapping("/api/hierarchy")
@RequiredArgsConstructor
@Tag(name = "Hierarchy Dashboards", description = "Per-level target vs team-disbursal dashboards")
@SecurityRequirement(name = "bearerAuth")
public class HierarchyDashboardController {

    private final HierarchyDashboardService hierarchyDashboardService;
    private final UserRepository userRepository;

    @GetMapping("/dashboard/me")
    @Operation(summary = "My target-flow dashboard",
               description = "My monthly target, my whole team's disbursal to date, and a per-direct-report breakdown. "
                       + "For Admin/HR the direct reports are the Managers; for a Manager they are the Team Leaders; "
                       + "for a Team Leader the ATLs; for an ATL the employees.")
    public ResponseEntity<ApiResponse<HierarchyDashboardResponse>> myDashboard(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        Long employeeId = resolveEmployeeId(currentUser);
        RoleType tier = resolveTier(currentUser);
        return ResponseEntity.ok(ApiResponse.success(
                hierarchyDashboardService.getMyDashboard(employeeId, tier, year, month)));
    }

    @GetMapping("/all-employees")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Admin/HR: all employees' target vs own disbursal",
               description = "Flat list of every active employee with their monthly target and their own approved "
                       + "disbursal for the period. The 'all employees' option, distinct from the tiered manager view.")
    public ResponseEntity<ApiResponse<List<HierarchyReportRow>>> allEmployees(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return ResponseEntity.ok(ApiResponse.success(
                hierarchyDashboardService.getAllEmployeesOverview(year, month)));
    }

    // ------------------------------------------------------------------ helpers

    /** The caller's highest level in the chain — drives which layer they see. */
    private RoleType resolveTier(UserPrincipal principal) {
        Set<String> authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        for (RoleType rt : List.of(RoleType.ROLE_ADMIN, RoleType.ROLE_HR, RoleType.ROLE_MANAGER,
                RoleType.ROLE_TEAM_LEADER, RoleType.ROLE_ATL, RoleType.ROLE_EMPLOYEE)) {
            if (authorities.contains(rt.name())) {
                return rt;
            }
        }
        return RoleType.ROLE_EMPLOYEE;
    }

    /** UserPrincipal carries the User row id; the service needs the Employee id. */
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
