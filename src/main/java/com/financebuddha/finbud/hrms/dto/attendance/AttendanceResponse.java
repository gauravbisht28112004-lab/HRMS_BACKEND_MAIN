package com.financebuddha.finbud.hrms.dto.attendance;

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
    private String employeeName;
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
}
