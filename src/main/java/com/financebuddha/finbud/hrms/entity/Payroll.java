package com.financebuddha.finbud.hrms.entity;

import com.financebuddha.finbud.hrms.entity.base.BaseEntity;
import com.financebuddha.finbud.hrms.enums.PayrollStatus;
import com.financebuddha.finbud.hrms.enums.SalaryStructureType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payroll")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Payroll extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "total_working_days", nullable = false)
    private Integer totalWorkingDays;

    @Column(name = "present_days", nullable = false, precision = 5, scale = 2)
    private BigDecimal presentDays;

    @Column(name = "absent_days", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal absentDays = BigDecimal.ZERO;

    @Column(name = "leave_days", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal leaveDays = BigDecimal.ZERO;

    @Column(name = "half_days", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal halfDays = BigDecimal.ZERO;

    @Column(name = "weekly_off_days")
    @Builder.Default
    private Integer weeklyOffDays = 0;

    @Column(name = "holidays")
    @Builder.Default
    private Integer holidays = 0;

    /**
     * @deprecated Legacy component-earned field. New CTC payrolls should use {@link #grossEarnings}.
     */
    @Deprecated
    @Column(name = "basic_earned", precision = 12, scale = 2)
    private BigDecimal basicEarned;

    /**
     * @deprecated Legacy component-earned field. See {@link #basicEarned}.
     */
    @Deprecated
    @Column(name = "hra_earned", precision = 12, scale = 2)
    private BigDecimal hraEarned;

    @Column(name = "da_earned", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal daEarned = BigDecimal.ZERO;

    @Column(name = "conveyance_earned", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal conveyanceEarned = BigDecimal.ZERO;

    @Column(name = "medical_earned", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal medicalEarned = BigDecimal.ZERO;

    @Column(name = "special_earned", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal specialEarned = BigDecimal.ZERO;

    @Column(name = "total_allowances", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAllowances = BigDecimal.ZERO;

    @Column(name = "gross_earnings", nullable = false, precision = 19, scale = 4)
    private BigDecimal grossEarnings;

    @Column(name = "pf_deduction", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal pfDeduction = BigDecimal.ZERO;

    @Column(name = "esi_deduction", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal esiDeduction = BigDecimal.ZERO;

    @Column(name = "pt_deduction", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal ptDeduction = BigDecimal.ZERO;

    @Column(name = "lop_deduction", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal lopDeduction = BigDecimal.ZERO;

    @Column(name = "other_deductions", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    @Column(name = "total_deductions", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDeductions;

    @Column(name = "net_pay", nullable = false, precision = 19, scale = 4)
    private BigDecimal netPay;

    @Column(name = "overtime_hours", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Column(name = "overtime_pay", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal overtimePay = BigDecimal.ZERO;

    // ------------------------------------------------------------------
    // Finbud CTC / NTH model (V4)
    // ------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "structure_type", length = 30)
    private SalaryStructureType structureType;

    @Column(name = "monthly_gross_ctc", precision = 19, scale = 4)
    private BigDecimal monthlyGrossCtc;

    @Column(name = "working_days")
    private Integer workingDays;

    @Column(name = "lop_days", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal lopDays = BigDecimal.ZERO;

    @Column(name = "employer_pf", precision = 19, scale = 4)
    private BigDecimal employerPf;

    @Column(name = "employee_pf", precision = 19, scale = 4)
    private BigDecimal employeePf;

    @Column(name = "employer_esi", precision = 19, scale = 4)
    private BigDecimal employerEsi;

    @Column(name = "employee_esi", precision = 19, scale = 4)
    private BigDecimal employeeEsi;

    @Column(name = "lwf_amount", precision = 19, scale = 4)
    private BigDecimal lwfAmount;

    @Column(name = "tds_amount", precision = 19, scale = 4)
    private BigDecimal tdsAmount;

    @Column(name = "incentive_amount", precision = 19, scale = 4)
    private BigDecimal incentiveAmount;

    /**
     * Reconciliation adjustment used to align computed NTH with HR-approved NTH
     * (see A3 in implementation plan — e.g. ₹150 rounding gap in Finbud Management).
     */
    @Column(name = "adjustments", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal adjustments = BigDecimal.ZERO;

    @Column(name = "adjustment_reason", length = 500)
    private String adjustmentReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PayrollStatus status = PayrollStatus.DRAFT;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Employee approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payslip_generated")
    @Builder.Default
    private Boolean payslipGenerated = false;

    @Column(name = "payslip_url", length = 500)
    private String payslipUrl;
}
