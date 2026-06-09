package com.financebuddha.finbud.hrms.entity;

import com.financebuddha.finbud.hrms.entity.base.BaseEntity;
import com.financebuddha.finbud.hrms.enums.RegularizationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Employee-filed request to correct attendance for a specific date.
 * Common reasons: forgot to punch in/out, phone died, punched from outside
 * geofence, network error. TL/HR/Admin approve or reject. On approval, the
 * AttendanceService writes (or updates) the corresponding {@link Attendance}
 * row with the requested timestamps.
 */
@Entity
@Table(name = "regularization_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RegularizationRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    // Link to the existing attendance row if the employee is asking to fix
    // an existing PENDING / REJECTED / MISSING_PUNCH record. Null when the
    // request is for a date with no row yet (e.g. auto-absent).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id")
    private Attendance attendance;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "requested_punch_in")
    private LocalDateTime requestedPunchIn;

    @Column(name = "requested_punch_out")
    private LocalDateTime requestedPunchOut;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RegularizationStatus status = RegularizationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private Employee reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;
}
