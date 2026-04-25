package com.financebuddha.finbud.hrms.entity;

import com.financebuddha.finbud.hrms.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Per-hour activity log for an employee. Pure data — no approval workflow.
 * Drives the daily / weekly commitment reports. The DB enforces uniqueness
 * on {@code (employee_id, work_date, hour_slot)} so re-submitting the same
 * slot updates the existing row rather than creating duplicates.
 */
@Entity
@Table(name = "hourly_updates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class HourlyUpdate extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    /** Free-form for now (e.g. "10:00-11:00"). Could later be an enum. */
    @Column(name = "hour_slot", nullable = false, length = 15)
    private String hourSlot;

    @Column(name = "calls_done", nullable = false)
    @Builder.Default
    private Integer callsDone = 0;

    @Column(name = "otps_achieved", nullable = false)
    @Builder.Default
    private Integer otpsAchieved = 0;

    @Column(name = "interested_customers", nullable = false)
    @Builder.Default
    private Integer interestedCustomers = 0;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
