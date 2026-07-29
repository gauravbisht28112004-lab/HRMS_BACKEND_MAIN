package com.financebuddha.finbud.hrms.enums;

/**
 * Types of in-app notifications the system can emit. Add new values when a
 * new event class starts emitting notifications — the listing UI just
 * reads the type string so it's extensible without schema churn.
 *
 * <p>Naming convention: {@code DOMAIN_EVENT}. Keep the domain prefix so
 * filtering notifications by domain stays trivial at the SQL level.
 */
public enum NotificationType {
    // Leave module (T2-3)
    LEAVE_APPLIED,
    LEAVE_APPROVED,
    LEAVE_REJECTED,
    LEAVE_CANCELLED,
    LEAVE_OVERRIDDEN
}
