package com.financebuddha.finbud.hrms.enums;

/**
 * Terminal attendance state for a given (employee, date). Only one per day.
 *
 * <p>PRESENT — employee punched in and out successfully (or HR/TL manually
 * regularized the row). HALF_DAY is derived from working hours.</p>
 *
 * <p>ABSENT — no valid punch on a working day. AUTO_ABSENT is set by the
 * nightly scheduler; HR can still override.</p>
 *
 * <p>MISSING_PUNCH — there is a punch-in but no punch-out by shift end.
 * Flagged for HR review; employee can file a regularization request.</p>
 *
 * <p>ON_LEAVE / HOLIDAY / WEEKLY_OFF are informational states the scheduler
 * writes (or derives on-the-fly in the UI) so HR sees a complete calendar.</p>
 */
public enum AttendanceStatus {
    PRESENT,
    ABSENT,
    HALF_DAY,
    ON_LEAVE,
    HOLIDAY,
    WEEKLY_OFF,
    PENDING,
    AUTO_ABSENT,
    MISSING_PUNCH
}
