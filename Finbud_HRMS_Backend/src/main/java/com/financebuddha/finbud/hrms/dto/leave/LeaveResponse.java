package com.financebuddha.finbud.hrms.dto.leave;

import com.financebuddha.finbud.hrms.enums.HalfDayType;
import com.financebuddha.finbud.hrms.enums.LeaveStatus;
import com.financebuddha.finbud.hrms.enums.LeaveType;
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
public class LeaveResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal daysRequested;
    private String reason;
    private String contactDuringLeave;
    private Long managerId;
    private String managerName;
    private LeaveStatus status;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private Boolean isHalfDay;
    private HalfDayType halfDayType;
    private LocalDateTime createdAt;
}
