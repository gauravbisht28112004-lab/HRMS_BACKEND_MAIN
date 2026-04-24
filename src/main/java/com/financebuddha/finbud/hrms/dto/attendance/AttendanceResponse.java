package com.financebuddha.finbud.hrms.dto.attendance;

import com.financebuddha.finbud.hrms.enums.AttendanceApprovalStatus;
import com.financebuddha.finbud.hrms.enums.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {

    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private String department;
    private String designation;
    private LocalDate attendanceDate;
    private LocalDateTime punchIn;
    private LocalDateTime punchOut;
    private BigDecimal workingHours;
    private AttendanceStatus status;
    private Boolean isLate;
    private Integer lateMinutes;
    private Boolean isEarlyLeave;
    private Integer earlyLeaveMinutes;
    private Boolean isHalfDay;
    private Boolean isOvertime;
    private BigDecimal overtimeHours;
    private String deviceId;
    private String notes;

    // Geo / approval surface
    private BigDecimal punchInLatitude;
    private BigDecimal punchInLongitude;
    private BigDecimal punchInAccuracyMeters;
    private BigDecimal punchOutLatitude;
    private BigDecimal punchOutLongitude;
    private BigDecimal punchOutAccuracyMeters;

    private AttendanceApprovalStatus approvalStatus;
    private Long approvedById;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String rejectionReason;

    private Boolean isAutoAbsent;
    private Boolean isMissingPunch;
    private Long manuallyEditedById;
    private String manuallyEditedByName;
    private LocalDateTime manuallyEditedAt;
}
