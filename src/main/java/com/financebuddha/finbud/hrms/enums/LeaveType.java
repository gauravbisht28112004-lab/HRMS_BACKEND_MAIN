package com.financebuddha.finbud.hrms.enums;

/**
 * Finbud leave types (locked 2026-04-24).
 *
 * <p>CASUAL and SICK share a single 6-day pool per year — the distinction is
 * kept so HR reports can tell them apart, but the underlying balance is
 * combined. PAID (a.k.a. "earned") has its own 6-day bucket. LOP (Loss of
 * Pay) is not pre-allocated — it accrues when requested and feeds payroll
 * salary deductions.
 *
 * <p>WFH was removed in V12: WFH is a working arrangement, not an absence.
 */
public enum LeaveType {
    CASUAL,
    SICK,
    PAID,
    LOP
}
