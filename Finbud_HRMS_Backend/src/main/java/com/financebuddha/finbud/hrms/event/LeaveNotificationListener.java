package com.financebuddha.finbud.hrms.event;

import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.LeaveRequest;
import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.enums.NotificationType;
import com.financebuddha.finbud.hrms.enums.RoleType;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.LeaveRequestRepository;
import com.financebuddha.finbud.hrms.repository.UserRepository;
import com.financebuddha.finbud.hrms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Fans out leave domain events to in-app notifications.
 *
 * <h2>Why TransactionPhase.AFTER_COMMIT</h2>
 * We only notify after the originating transaction (approve / reject / etc.)
 * has actually committed to the database. If the leave write rolls back,
 * recipients never see a phantom "approved" notification.
 *
 * <h2>Recipient model</h2>
 * For every leave event we compute a set of employee ids to notify, minus
 * the actor who already knows what they just did:
 *
 * <ul>
 *   <li>the applicant (the employee whose leave it is)</li>
 *   <li>their direct manager, if any</li>
 *   <li>every active HR user's employee</li>
 *   <li>every active Admin user's employee</li>
 * </ul>
 *
 * <p>This is deliberately broad — leave decisions are not secret and the
 * approver queue is global (per the design). Narrowing comes later if
 * needed (e.g. department-scoped HR).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeaveNotificationListener {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // -----------------------------------------------------------------
    // Event handlers
    // -----------------------------------------------------------------

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplied(LeaveEvents.LeaveAppliedEvent event) {
        LeaveRequest leave = leaveRequestRepository.findById(event.leaveRequestId()).orElse(null);
        if (leave == null) return;

        String applicantName = safeName(leave.getEmployee());
        String title = "%s applied for %s leave".formatted(applicantName, leave.getLeaveType());
        String body = leaveWindowSummary(leave);

        // Applied events notify the approvers, not the applicant themselves.
        Set<Long> recipients = approverRecipients(leave);
        recipients.remove(event.applicantEmployeeId());

        fanOut(recipients, NotificationType.LEAVE_APPLIED, title, body, leave.getId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApproved(LeaveEvents.LeaveApprovedEvent event) {
        LeaveRequest leave = leaveRequestRepository.findById(event.leaveRequestId()).orElse(null);
        if (leave == null) return;

        String approverName = resolveEmployeeName(event.approverEmployeeId());
        String applicantName = safeName(leave.getEmployee());
        String title = "%s's leave approved by %s".formatted(applicantName, approverName);
        String body = leaveWindowSummary(leave);

        Set<Long> recipients = allStakeholders(leave);
        recipients.remove(event.approverEmployeeId());

        fanOut(recipients, NotificationType.LEAVE_APPROVED, title, body, leave.getId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRejected(LeaveEvents.LeaveRejectedEvent event) {
        LeaveRequest leave = leaveRequestRepository.findById(event.leaveRequestId()).orElse(null);
        if (leave == null) return;

        String rejectorName = resolveEmployeeName(event.rejectorEmployeeId());
        String applicantName = safeName(leave.getEmployee());
        String title = "%s's leave rejected by %s".formatted(applicantName, rejectorName);
        String body = leaveWindowSummary(leave)
                + (event.rejectionReason() != null ? " — " + event.rejectionReason() : "");

        Set<Long> recipients = allStakeholders(leave);
        recipients.remove(event.rejectorEmployeeId());

        fanOut(recipients, NotificationType.LEAVE_REJECTED, title, body, leave.getId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCancelled(LeaveEvents.LeaveCancelledEvent event) {
        LeaveRequest leave = leaveRequestRepository.findById(event.leaveRequestId()).orElse(null);
        if (leave == null) return;

        String cancellerName = resolveEmployeeName(event.cancellerEmployeeId());
        String applicantName = safeName(leave.getEmployee());
        String title = "%s's leave cancelled by %s".formatted(applicantName, cancellerName);
        String body = leaveWindowSummary(leave);

        Set<Long> recipients = approverRecipients(leave);
        recipients.remove(event.cancellerEmployeeId());

        fanOut(recipients, NotificationType.LEAVE_CANCELLED, title, body, leave.getId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOverridden(LeaveEvents.LeaveOverriddenEvent event) {
        LeaveRequest leave = leaveRequestRepository.findById(event.leaveRequestId()).orElse(null);
        if (leave == null) return;

        String overriderName = resolveEmployeeName(event.overriderEmployeeId());
        String applicantName = safeName(leave.getEmployee());
        String title = "%s's leave changed from %s to %s by %s"
                .formatted(applicantName, event.oldStatus(), event.newStatus(), overriderName);
        String body = leaveWindowSummary(leave) + " — " + event.reason();

        // Override notifies everyone who cares — applicant and all approvers,
        // minus the actor. They might have approved earlier and HR just
        // reversed it; that's exactly the kind of thing we want surfaced.
        Set<Long> recipients = allStakeholders(leave);
        recipients.remove(event.overriderEmployeeId());

        fanOut(recipients, NotificationType.LEAVE_OVERRIDDEN, title, body, leave.getId());
    }

    // -----------------------------------------------------------------
    // Recipient resolution
    // -----------------------------------------------------------------

    /** Everyone who should hear about a status change: applicant + approvers. */
    private Set<Long> allStakeholders(LeaveRequest leave) {
        Set<Long> set = approverRecipients(leave);
        if (leave.getEmployee() != null) set.add(leave.getEmployee().getId());
        return set;
    }

    /** Approvers only: direct manager + all HR + all Admin. */
    private Set<Long> approverRecipients(LeaveRequest leave) {
        Set<Long> set = new HashSet<>();
        if (leave.getManager() != null) set.add(leave.getManager().getId());
        addRoleEmployees(set, RoleType.ROLE_HR);
        addRoleEmployees(set, RoleType.ROLE_ADMIN);
        return set;
    }

    private void addRoleEmployees(Set<Long> set, RoleType role) {
        List<User> users = userRepository.findActiveUsersByRoleName(role);
        for (User u : users) {
            if (u.getEmployee() != null) set.add(u.getEmployee().getId());
        }
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private void fanOut(Set<Long> recipients, NotificationType type, String title, String body, Long leaveId) {
        for (Long recipientId : recipients) {
            notificationService.notify(recipientId, type, title, body, "LEAVE_REQUEST", leaveId);
        }
        log.debug("Fanned out {} to {} recipient(s) for leave id={}", type, recipients.size(), leaveId);
    }

    private String leaveWindowSummary(LeaveRequest leave) {
        if (Boolean.TRUE.equals(leave.getIsHalfDay())) {
            return "Half day on %s".formatted(leave.getStartDate());
        }
        if (leave.getStartDate() != null && leave.getStartDate().equals(leave.getEndDate())) {
            return "%s".formatted(leave.getStartDate());
        }
        return "%s → %s".formatted(leave.getStartDate(), leave.getEndDate());
    }

    private String safeName(Employee e) {
        if (e == null) return "Someone";
        String full = e.getFullName();
        return full != null ? full : ("Employee " + e.getId());
    }

    private String resolveEmployeeName(Long employeeId) {
        if (employeeId == null) return "HR";
        Optional<Employee> employee = employeeRepository.findById(employeeId);
        return employee.map(this::safeName).orElse("an approver");
    }
}
