package com.financebuddha.finbud.hrms.entity;

import com.financebuddha.finbud.hrms.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Per-employee per-month sales targets. Set by TL for direct reports;
 * HR/Admin can override. Achieved values are NOT stored here — they are
 * computed at read-time from {@code daily_commitments.actual_disbursal_amount}.
 */
@Entity
@Table(name = "monthly_targets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MonthlyTarget extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "target_disbursal_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal targetDisbursalAmount = BigDecimal.ZERO;

    @Column(name = "target_logins", nullable = false)
    @Builder.Default
    private Integer targetLogins = 0;

    /** Whoever last set/updated this target. Useful for audit + UI labels. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_by_id")
    private Employee setBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
