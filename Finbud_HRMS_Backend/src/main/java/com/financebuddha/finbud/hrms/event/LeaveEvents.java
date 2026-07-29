package com.financebuddha.finbud.hrms.event;

import com.financebuddha.finbud.hrms.enums.LeaveStatus;

/**
 * Domain events published by {@code LeaveServiceImpl} after each successful
 * state transition. Subscribers (notification listener, future email
 * listener, attendance bridge, audit log) react without creating a hard
 * dependency on each other.
 *
 * <p>All events are plain Java records — keep them lightweight and carry
 * just the IDs the listeners need. Listeners re-read the entity when they
 * need full detail, which avoids stale-data-in-event problems.
 *
 * <p>Published with Spring's {@code ApplicationEventPublisher}, consumed
 * with {@link org.springframework.transaction.event.TransactionalEventListener}
 * in phase {@code AFTER_COMMIT} — so notifications are only emitted when
 * the originating DB transaction actually committed. No orphan notifications
 * if the leave update rolls back.
 */
public final class LeaveEvents {

    private LeaveEvents() {}

    /** An employee submitted a new leave request. */
    public record LeaveAppliedEvent(
            Long leaveRequestId,
            Long applicantEmployeeId
    ) {}

    /** A TL / HR / Admin approved a pending leave request. */
    public record LeaveApprovedEvent(
            Long leaveRequestId,
            Long applicantEmployeeId,
            Long approverEmployeeId
    ) {}

    /** A TL / HR / Admin rejected a pending leave request. */
    public record LeaveRejectedEvent(
            Long leaveRequestId,
            Long applicantEmployeeId,
            Long rejectorEmployeeId,
            String rejectionReason
    ) {}

    /** A leave request was cancelled (by the employee, or HR override in Phase 4). */
    public record LeaveCancelledEvent(
            Long leaveRequestId,
            Long applicantEmployeeId,
            Long cancellerEmployeeId
    ) {}

    /**
     * HR / Admin overrode an already-decided leave. Carries the previous
     * status so the listener can render a useful notification body
     * ("Akash's leave was changed from APPROVED to REJECTED by HR").
     */
    public record LeaveOverriddenEvent(
            Long leaveRequestId,
            Long applicantEmployeeId,
            Long overriderEmployeeId,
            LeaveStatus oldStatus,
            LeaveStatus newStatus,
            String reason
    ) {}
}
