package com.financebuddha.finbud.hrms.dto.payroll;

import com.financebuddha.finbud.hrms.enums.PayrollStatus;
import com.financebuddha.finbud.hrms.enums.SalaryStructureType;
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

    // ------------------------------------------------------------------
    // CTC / NTH model context
    // ------------------------------------------------------------------
    private SalaryStructureType structureType;
    private BigDecimal monthlyGrossCtc;
    private Integer workingDays;
    private BigDecimal lopDays;

    // Attendance summary
    private Integer totalWorkingDays;
    private BigDecimal presentDays;
    private BigDecimal absentDays;
    private BigDecimal leaveDays;
    private BigDecimal halfDays;
    private Integer weeklyOffDays;
    private Integer holidays;

    // ------------------------------------------------------------------
    // Earnings
    // ------------------------------------------------------------------
    private BigDecimal basicEarned;     // legacy; null for CTC-model payrolls
    private BigDecimal hraEarned;       // legacy; null for CTC-model payrolls
    private BigDecimal daEarned;
    private BigDecimal conveyanceEarned;
    private BigDecimal medicalEarned;
    private BigDecimal specialEarned;
    private BigDecimal totalAllowances;
    private BigDecimal grossEarnings;

    // ------------------------------------------------------------------
    // Deductions — CTC model
    // ------------------------------------------------------------------
    private BigDecimal employerPf;
    private BigDecimal employeePf;
    private BigDecimal employerEsi;
    private BigDecimal employeeEsi;
    private BigDecimal lwfAmount;
    private BigDecimal tdsAmount;

    // Legacy / component deductions
    private BigDecimal pfDeduction;
    private BigDecimal esiDeduction;
    private BigDecimal ptDeduction;
    private BigDecimal lopDeduction;
    private BigDecimal otherDeductions;
    private BigDecimal totalDeductions;

    // Incentives / reconciliation
    private BigDecimal incentiveAmount;
    private BigDecimal adjustments;
    private String adjustmentReason;

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
    private LocalDateTime paidAt;
    private Boolean payslipGenerated;
    private String payslipUrl;
}
