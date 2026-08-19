package com.financebuddha.finbud.hrms.security;

import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.exception.ForbiddenException;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Custom authorization helper exposed to {@code @PreAuthorize} expressions
 * as {@code @authz}. Used to enforce horizontal access control — the rule
 * that an employee may read THEIR OWN attendance / leave / payroll record
 * but not another employee's, even though they all carry the same role.
 *
 * <p>Typical usage on a controller method:
 * <pre>
 *   &#64;PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER') or @authz.isOwner(#employeeId)")
 * </pre>
 *
 * <p>Spring Security evaluates this expression at request time. If the
 * authenticated principal's linked employee primary-key id matches the
 * requested {@code employeeId}, the call proceeds; otherwise a 403 is
 * returned by the security infrastructure.
 *
 * <p>This is defense IN DEPTH on top of the {@code SecurityFilterChain}
 * URL rules in {@link SecurityConfig}. Filter-chain rules cover the
 * common case of "block all non-HR/Admin from this URL pattern"; this
 * helper covers the more granular "allow only the owning employee".
 */
@Component("authz")
@RequiredArgsConstructor
public class AuthzService {

    private final EmployeeRepository employeeRepository;

    /**
     * True if the authenticated user is the employee identified by
     * {@code employeeId} (compared against {@link UserPrincipal#getEmployeePrimaryId()}).
     * Returns false for unauthenticated requests, anonymous principals,
     * or principals with no linked Employee row.
     */
    public boolean isOwner(Long employeeId) {
        if (employeeId == null) {
            return false;
        }
        UserPrincipal principal = currentPrincipal();
        if (principal == null || principal.getEmployeePrimaryId() == null) {
            return false;
        }
        return employeeId.equals(principal.getEmployeePrimaryId());
    }

    /**
     * True if the authenticated principal has any of the given role names
     * (without the {@code ROLE_} prefix — pass {@code "ADMIN"}, not
     * {@code "ROLE_ADMIN"}). False for unauthenticated requests.
     */
    public boolean hasAnyRole(String... roles) {
        UserPrincipal principal = currentPrincipal();
        if (principal == null || principal.getAuthorities() == null) {
            return false;
        }
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> Arrays.stream(roles)
                        .anyMatch(role -> authority.equals("ROLE_" + role)));
    }

    // =====================================================================
    // Service-layer guards — defense IN DEPTH
    //
    // The methods below throw {@link ForbiddenException} (HTTP 403) instead
    // of returning a boolean. Service implementations call these as the
    // first line of every read method that takes an employeeId / attendanceId
    // / leaveId, so even if a future controller forgets {@code @PreAuthorize}
    // (or a non-web caller invokes the service directly — schedulers, the AI
    // assistant, internal jobs), the service still refuses to leak data.
    //
    // Privileged roles (ADMIN / HR / MANAGER) bypass the ownership check.
    // For everyone else, the principal's linked employee primary-key id /
    // owned-record check must match.
    // =====================================================================

    /**
     * Allow if the caller has ADMIN/HR/MANAGER, OR is the employee identified
     * by {@code employeeId}. Throws otherwise.
     */
    public void requireOwnerOrPrivileged(Long employeeId) {
        if (hasAnyRole("ADMIN", "HR", "MANAGER") || isOwner(employeeId)) {
            return;
        }
        throw new ForbiddenException(
                "Not authorized to access data for employee " + employeeId);
    }

    // =====================================================================
    // ATL team-scope guards
    //
    // An ATL (Assistant Team Leader) may only act on their own direct
    // reports — the employees whose {@code manager} is the ATL. Privileged
    // roles (ADMIN / HR / MANAGER) stay unrestricted, preserving existing
    // behaviour. Callers holding none of these roles are refused outright.
    // =====================================================================

    /**
     * Allow if the caller is ADMIN/HR/MANAGER (unrestricted), OR is an ATL
     * who directly manages {@code employee}. Throws {@link ForbiddenException}
     * otherwise. Used by the monthly-target and daily-commitment services so
     * an ATL cannot assign a target to — or approve a commitment for — an
     * employee outside their own team.
     */
    public void requireCanManageEmployee(Employee employee) {
        // ADMIN / HR keep the org-wide override (e.g. Admin sets a Manager's
        // target, HR corrects anyone's).
        if (hasAnyRole("ADMIN", "HR")) {
            return;
        }
        // Every supervisor level in the chain — MANAGER, TEAM_LEADER, ATL — may
        // only assign to / act on their OWN direct reports. This is what makes
        // the target cascade one hop at a time: a Manager assigns to the Team
        // Leaders directly under them, a Team Leader to their ATLs, an ATL to
        // their employees — never skipping a level.
        if (hasAnyRole("MANAGER", "TEAM_LEADER", "ATL")) {
            UserPrincipal principal = currentPrincipal();
            if (principal != null && principal.getEmployeePrimaryId() != null
                    && employee != null && employee.getManager() != null
                    && principal.getEmployeePrimaryId().equals(employee.getManager().getId())) {
                return;
            }
            throw new ForbiddenException("You can only manage your own direct reports");
        }
        throw new ForbiddenException("Not authorized to manage this employee");
    }

    /**
     * For manager-id-scoped list endpoints (pending queue, team snapshot,
     * team monthly targets): ADMIN/HR/MANAGER may query any manager's team;
     * an ATL may query only their own — {@code managerId} equal to their
     * employee primary-key id. Throws {@link ForbiddenException} otherwise.
     */
    public void requireCanActAsManager(Long managerId) {
        if (hasAnyRole("ADMIN", "HR")) {
            return;
        }
        // A supervisor (MANAGER / TEAM_LEADER / ATL) may only list their OWN
        // team's targets — managerId must be their own employee id. This keeps
        // each level's view scoped to their direct reports (no peeking at a
        // sibling's team, no overlap between levels).
        if (hasAnyRole("MANAGER", "TEAM_LEADER", "ATL")) {
            UserPrincipal principal = currentPrincipal();
            if (principal != null && managerId != null
                    && managerId.equals(principal.getEmployeePrimaryId())) {
                return;
            }
            throw new ForbiddenException("You can only view your own team");
        }
        throw new ForbiddenException("Not authorized to view this team");
    }

    // =====================================================================
    // Report-export guards
    //
    // The Excel exports span a supervisor's WHOLE subtree, not just their
    // direct reports, so the ownership rule has to be expressed against the
    // subtree too. Roles alone are not enough here: every Manager carries
    // ROLE_MANAGER, so a plain hasRole('MANAGER') check would let any manager
    // download any other manager's team by editing the id in the URL.
    // =====================================================================

    /**
     * Guard for the team export. ADMIN/HR may export any node; a supervisor
     * (MANAGER / TEAM_LEADER / ATL) may export only their own subtree, i.e.
     * {@code rootId} must be their own employee primary-key id. Employees have
     * no team and are refused.
     *
     * <p>Identical rule to {@link #requireCanActAsManager(Long)}; kept as a
     * separate named method so the export path reads explicitly and can diverge
     * later without disturbing the commitment-queue endpoints.
     */
    public void requireCanExportTeamReport(Long rootId) {
        requireCanActAsManager(rootId);
    }

    /**
     * Guard for the single-employee export. Allowed when the caller is
     * ADMIN/HR, is the employee themselves, or is a supervisor with that
     * employee somewhere beneath them in the reporting tree — a Manager
     * reaching an employee through a TL and an ATL is fine; reaching into a
     * sibling manager's branch is not.
     */
    public void requireCanExportEmployeeReport(Long employeeId) {
        if (hasAnyRole("ADMIN", "HR") || isOwner(employeeId)) {
            return;
        }
        if (hasAnyRole("MANAGER", "TEAM_LEADER", "ATL")) {
            UserPrincipal principal = currentPrincipal();
            Long callerId = principal != null ? principal.getEmployeePrimaryId() : null;
            if (callerId != null && employeeId != null
                    && employeeRepository.existsInSubtree(callerId, employeeId)) {
                return;
            }
            throw new ForbiddenException(
                    "You can only export reports for employees in your own team");
        }
        throw new ForbiddenException(
                "Not authorized to export data for employee " + employeeId);
    }

    private UserPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object p = auth.getPrincipal();
        return (p instanceof UserPrincipal) ? (UserPrincipal) p : null;
    }
}
