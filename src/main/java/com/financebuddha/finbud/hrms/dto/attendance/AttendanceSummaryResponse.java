package com.financebuddha.finbud.hrms.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryResponse {

    private Long employeeId;
    private String employeeName;
    private String month;
    private Integer year;
    private Integer totalDays;
    private Integer presentDays;
    private Integer absentDays;
    private Integer halfDays;
    private Integer lateCount;
    private BigDecimal totalOvertimeHours;
}
