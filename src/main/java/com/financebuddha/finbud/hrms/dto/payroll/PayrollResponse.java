package com.financebuddha.finbud.hrms.dto.payroll;

import com.financebuddha.finbud.hrms.enums.PayrollStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeIdCode;
    private String departmentName;
    private Integer month;
    private Integer year;
    private String monthYear;

    // Attendance summary
    private Integer totalWorkingDays;
    private BigDecimal presentDays;
    private BigDecimal absentDays;
    private BigDecimal leaveDays;
    private BigDecimal halfDays;
    private Integer weeklyOffDays;

    // Earnings
    private BigDecimal basicEarned;
    private BigDecimal hraEarned;
    private BigDecimal daEarned;
    private BigDecimal conveyanceEarned;
    private BigDecimal medicalEarned;
    private BigDecimal specialEarned;
    private BigDecimal totalAllowances;
    private BigDecimal grossEarnings;

    // Deductions
    private BigDecimal pfDeduction;
    private BigDecimal esiDeduction;
    private BigDecimal ptDeduction;
    private BigDecimal lopDeduction;
    private BigDecimal otherDeductions;
    private BigDecimal totalDeductions;

    // Overtime
    private BigDecimal overtimeHours;
    private BigDecimal overtimePay;

    // Net Pay
    private BigDecimal netPay;
    private String netPayInWords;

    // Status
    private PayrollStatus status;
    private LocalDateTime generatedAt;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private Boolean payslipGenerated;
    private String payslipUrl;
}
