package com.financebuddha.finbud.hrms.entity;

import com.financebuddha.finbud.hrms.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Company-wide public holiday. The nightly auto-Absent scheduler skips
 * these dates entirely (so employees don't get auto-marked Absent on
 * Republic Day, for instance). Also surfaced on the employee calendar.
 */
@Entity
@Table(name = "public_holidays")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PublicHoliday extends BaseEntity {

    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate holidayDate;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Optional holidays (e.g. Rakshabandhan, Diwali second-day) show on the
    // calendar but do NOT block the auto-Absent scheduler. Required holidays
    // are counted as paid-off and skip the scheduler.
    @Column(name = "is_optional", nullable = false)
    @Builder.Default
    private Boolean isOptional = false;
}
