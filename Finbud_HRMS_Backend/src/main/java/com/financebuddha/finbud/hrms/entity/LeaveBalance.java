package com.financebuddha.finbud.hrms.entity;

import com.financebuddha.finbud.hrms.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Per-employee, per-calendar-year leave balance snapshot.
 *
 * <h2>Finbud policy (locked 2026-04-24)</h2>
 * <ul>
 *   <li><strong>Casual + Sick</strong> share a single 6-day pool
 *       ({@code casual_sick_allocated}). The employee picks CASUAL or SICK at
 *       request time for reporting, but both deduct from the same bucket.</li>
 *   <li><strong>Paid / Earned leave</strong>: 6 days / calendar year.</li>
 *   <li><strong>LOP (Loss of Pay)</strong>: not pre-allocated. Accrues only
 *       when an employee's bucket is exhausted or an approver explicitly marks
 *       the request LOP. Feeds into payroll salary deduction.</li>
 *   <li><strong>WFH</strong>: removed as a leave type — it's a working
 *       arrangement, not an absence.</li>
 *   <li><strong>Carry-forward</strong>: capped at one full year's allocation
 *       per type. Excess lapses on Jan 1 (calendar-year reset).</li>
 * </ul>
 *
 * <p>The {@code *_carried_forward} columns are informational — they record
 * how many days came across from the prior year so reports can show
 * "fresh 6 + carried 4 = effective 10". The {@code *_allocated} columns
 * already include the carry-forward.
 */
@Entity
@Table(name = "leave_balances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LeaveBalance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "year", nullable = false)
    private Integer year;

    // -------- Casual + Sick combined pool ----------------------------------

    /** Total days available in the casual+sick pool for the year. */
    @Column(name = "casual_sick_allocated", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal casualSickAllocated = new BigDecimal("6.00");

    /** Days already taken from the casual+sick pool in the year. */
    @Column(name = "casual_sick_used", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal casualSickUsed = BigDecimal.ZERO;

    /** Informational: how many days of the allocated pool came from the
     * prior year's unused balance (capped at one year's allocation). */
    @Column(name = "casual_sick_carried_forward", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal casualSickCarriedForward = BigDecimal.ZERO;

    // -------- Paid / Earned leave -----------------------------------------

    @Column(name = "paid_leave_allocated", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal paidLeaveAllocated = new BigDecimal("6.00");

    @Column(name = "paid_leave_used", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal paidLeaveUsed = BigDecimal.ZERO;

    @Column(name = "paid_leave_carried_forward", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal paidLeaveCarriedForward = BigDecimal.ZERO;

    // -------- LOP (Loss of Pay) — tracker only, no allocation --------------

    @Column(name = "lop_days", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal lopDays = BigDecimal.ZERO;

    // -------- Derived getters ---------------------------------------------

    public BigDecimal getCasualSickBalance() {
        return casualSickAllocated.subtract(casualSickUsed);
    }

    public BigDecimal getPaidLeaveBalance() {
        return paidLeaveAllocated.subtract(paidLeaveUsed);
    }
}
