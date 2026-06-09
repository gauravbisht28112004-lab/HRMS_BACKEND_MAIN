package com.financebuddha.finbud.hrms.enums;

/**
 * Which side of a {@code LeaveBalance} an HR adjustment targets.
 *
 * <ul>
 *   <li>{@link #CASUAL_SICK} — changes {@code casual_sick_allocated}.</li>
 *   <li>{@link #PAID} — changes {@code paid_leave_allocated}.</li>
 *   <li>{@link #LOP} — changes {@code lop_days} (rare; only for
 *       correcting mistakes — LOP is usually driven by the leave flow).</li>
 * </ul>
 */
public enum LeaveBalanceBucket {
    CASUAL_SICK,
    PAID,
    LOP
}
