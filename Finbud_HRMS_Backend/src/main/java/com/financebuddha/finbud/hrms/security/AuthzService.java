package com.financebuddha.finbud.hrms.security;

import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.exception.ForbiddenException;
import com.financebuddha.finbud.hrms.repository.AttendanceRepository;
import com.financebuddha.finbud.hrms.repository.LeaveRequestRepository;
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

    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;

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
     * True if the authenticated user owns the attendance row identified by
     * {@code attendanceId}. Looks up the row and compares its
     * {@code employee.id} against the principal's employee primary-key id.
     * Returns false (rather than throwing) for missing rows so the caller
     * sees a clean 403 instead of a 500.
     */
    public boolean ownsAttendance(Long attendanceId) {
        if (attendanceId == null) {
            return false;
        }
        UserPrincipal principal = currentPrincipal();
        if (principal == null || principal.getEmployeePrimaryId() == null) {
            return false;
        }
        return attendanceRepository.findById(attendanceId)
                .map(a -> a.getEmployee() != null
                        && principal.getEmployeePrimaryId().equals(a.getEmployee().getId()))
                .orElse(false);
    }

    /**
     * True if the authenticated user owns the leave request identified by
     * {@code leaveRequestId}.
     */
    public boolean ownsLeave(Long leaveRequestId) {
        if (leaveRequestId == null) {
            return false;
        }
        UserPrincipal principal = currentPrincipal();
        if (principal == null || principal.getEmployeePrimaryId() == null) {
            return false;
        }
        return leaveRequestRepository.findById(leaveRequestId)
                .map(l -> l.getEmployee() != null
                        && principal.getEmployeePrimaryId().equals(l.getEmployee().getId()))
                .orElse(false);
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

    /**
     * Allow if the caller has ADMIN/HR/MANAGER, OR is the employee that owns
     * the attendance row identified by {@code attendanceId}. Throws otherwise.
     */
    public void requireOwnsAttendanceOrPrivileged(Long attendanceId) {
        if (hasAnyRole("ADMIN", "HR", "MANAGER") || ownsAttendance(attendanceId)) {
            return;
        }
        throw new ForbiddenException(
                "Not authorized to access attendance record " + attendanceId);
    }

    /**
     * Allow if the caller has ADMIN/HR/MANAGER, OR is the employee that owns
     * the leave request identified by {@code leaveRequestId}. Throws otherwise.
     */
    public void requireOwnsLeaveOrPrivileged(Long leaveRequestId) {
        if (hasAnyRole("ADMIN", "HR", "MANAGER") || ownsLeave(leaveRequestId)) {
            return;
        }
        throw new ForbiddenException(
                "Not authorized to access leave request " + leaveRequestId);
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
        if (hasAnyRole("ADMIN", "HR", "MANAGER")) {
            return;
        }
        if (hasAnyRole("ATL")) {
            UserPrincipal principal = currentPrincipal();
            if (principal != null && principal.getEmployeePrimaryId() != null
                    && employee != null && employee.getManager() != null
                    && principal.getEmployeePrimaryId().equals(employee.getManager().getId())) {
                return;
            }
            throw new ForbiddenException("An ATL can only manage employees in their own team");
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
        if (hasAnyRole("ADMIN", "HR", "MANAGER")) {
            return;
        }
        if (hasAnyRole("ATL")) {
            UserPrincipal principal = currentPrincipal();
            if (principal != null && managerId != null
                    && managerId.equals(principal.getEmployeePrimaryId())) {
                return;
            }
            throw new ForbiddenException("An ATL can only view their own team");
        }
        throw new ForbiddenException("Not authorized to view this team");
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
