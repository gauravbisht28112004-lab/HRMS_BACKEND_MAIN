package com.financebuddha.finbud.hrms.entity;

import com.financebuddha.finbud.hrms.entity.base.BaseEntity;
import com.financebuddha.finbud.hrms.enums.CommitmentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One commitment row per employee per workday — captures the day's
 * targets and end-of-day actuals plus the TL approval workflow.
 *
 * <p>The DB enforces {@code UNIQUE (employee_id, work_date)} so attempting
 * to create a second commitment for the same day surfaces as a 409 from
 * the upstream constraint, which the service catches and re-emits as a
 * BadRequestException.
 */
@Entity
@Table(name = "daily_commitments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DailyCommitment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    // -------- Targets (set in the morning) ---------------------------------

    @Column(name = "target_calls", nullable = false)
    @Builder.Default
    private Integer targetCalls = 0;

    @Column(name = "target_otps", nullable = false)
    @Builder.Default
    private Integer targetOtps = 0;

    @Column(name = "target_interested_customers", nullable = false)
    @Builder.Default
    private Integer targetInterestedCustomers = 0;

    @Column(name = "target_disbursal_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal targetDisbursalAmount = BigDecimal.ZERO;

    // -------- Actuals (filled at end of day) -------------------------------

    @Column(name = "actual_calls")
    @Builder.Default
    private Integer actualCalls = 0;

    @Column(name = "actual_otps")
    @Builder.Default
    private Integer actualOtps = 0;

    @Column(name = "actual_interested_customers")
    @Builder.Default
    private Integer actualInterestedCustomers = 0;

    @Column(name = "actual_disbursal_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal actualDisbursalAmount = BigDecimal.ZERO;

    // -------- Workflow -----------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private CommitmentStatus status = CommitmentStatus.DRAFT;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private Employee approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
