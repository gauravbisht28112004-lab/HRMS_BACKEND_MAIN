package com.financebuddha.finbud.hrms.entity;

import com.financebuddha.finbud.hrms.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shift_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ShiftType extends BaseEntity {

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "break_duration_minutes", nullable = false)
    @Builder.Default
    private Integer breakDurationMinutes = 60;

    @Column(name = "grace_period_minutes", nullable = false)
    @Builder.Default
    private Integer gracePeriodMinutes = 10;

    @ElementCollection
    @CollectionTable(name = "shift_weekly_off_days", joinColumns = @JoinColumn(name = "shift_type_id"))
    @Column(name = "off_day")
    @Builder.Default
    private List<Integer> weeklyOffDays = new ArrayList<>();

    @Column(name = "is_night_shift")
    @Builder.Default
    private Boolean isNightShift = false;

    @Column(name = "overtime_threshold_hours", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal overtimeThresholdHours = new BigDecimal("8.00");

    @OneToMany(mappedBy = "shiftType")
    @Builder.Default
    private List<ShiftAssignment> assignments = new ArrayList<>();
}
