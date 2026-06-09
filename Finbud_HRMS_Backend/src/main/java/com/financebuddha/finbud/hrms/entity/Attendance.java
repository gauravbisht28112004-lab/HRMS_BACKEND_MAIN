package com.financebuddha.finbud.hrms.entity;

import com.financebuddha.finbud.hrms.entity.base.BaseEntity;
import com.financebuddha.finbud.hrms.enums.AttendanceApprovalStatus;
import com.financebuddha.finbud.hrms.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@TableGenerator(
        name = "attendance_gen",
        table = "id_generator",
        pkColumnName = "gen_name",
        valueColumnName = "gen_value",
        pkColumnValue = "attendance_seq",
        allocationSize = 1
)
public class Attendance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_type_id")
    private ShiftType shiftType;

    @Column(name = "punch_in")
    private LocalDateTime punchIn;

    @Column(name = "punch_out")
    private LocalDateTime punchOut;

    @Column(name = "working_hours", precision = 4, scale = 2)
    private BigDecimal workingHours;

    @Column(name = "break_hours", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal breakHours = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Column(name = "is_late")
    @Builder.Default
    private Boolean isLate = false;

    @Column(name = "late_minutes")
    @Builder.Default
    private Integer lateMinutes = 0;

    @Column(name = "is_early_leave")
    @Builder.Default
    private Boolean isEarlyLeave = false;

    @Column(name = "early_leave_minutes")
    @Builder.Default
    private Integer earlyLeaveMinutes = 0;

    @Column(name = "is_half_day")
    @Builder.Default
    private Boolean isHalfDay = false;

    @Column(name = "is_overtime")
    @Builder.Default
    private Boolean isOvertime = false;

    @Column(name = "overtime_hours", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Column(name = "device_id", length = 50)
    private String deviceId;

    @Column(name = "punch_in_location", length = 255)
    private String punchInLocation;

    @Column(name = "punch_out_location", length = 255)
    private String punchOutLocation;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ---------------------------------------------------------------------
    // Geo-location captured from the browser for each punch. Latitude &
    // longitude are checked against the employee's OfficeLocation when
    // enforce_geofence is on. Accuracy helps us log how trustworthy the
    // reading is (mobile GPS can be metres or kilometres off).
    // ---------------------------------------------------------------------

    @Column(name = "punch_in_latitude", precision = 10, scale = 7)
    private BigDecimal punchInLatitude;

    @Column(name = "punch_in_longitude", precision = 10, scale = 7)
    private BigDecimal punchInLongitude;

    @Column(name = "punch_in_accuracy_meters", precision = 8, scale = 2)
    private BigDecimal punchInAccuracyMeters;

    @Column(name = "punch_out_latitude", precision = 10, scale = 7)
    private BigDecimal punchOutLatitude;

    @Column(name = "punch_out_longitude", precision = 10, scale = 7)
    private BigDecimal punchOutLongitude;

    @Column(name = "punch_out_accuracy_meters", precision = 8, scale = 2)
    private BigDecimal punchOutAccuracyMeters;

    // ---------------------------------------------------------------------
    // Approval workflow. A portal-marked attendance row is PENDING until a
    // TL / HR / Admin approves it. Rejections keep the row (for audit)
    // with a reason. Historical pre-V11 rows are migrated to APPROVED.
    // ---------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private AttendanceApprovalStatus approvalStatus = AttendanceApprovalStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private Employee approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // ---------------------------------------------------------------------
    // Scheduler bookkeeping. is_auto_absent is set by the nightly job that
    // creates ABSENT rows for missing punches. is_missing_punch is set when
    // punch-in exists but punch-out does not by the shift end time. HR can
    // override either, in which case manually_edited_by* captures who did.
    // ---------------------------------------------------------------------

    @Column(name = "is_auto_absent", nullable = false)
    @Builder.Default
    private Boolean isAutoAbsent = false;

    @Column(name = "is_missing_punch", nullable = false)
    @Builder.Default
    private Boolean isMissingPunch = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manually_edited_by_id")
    private Employee manuallyEditedBy;

    @Column(name = "manually_edited_at")
    private LocalDateTime manuallyEditedAt;
}
