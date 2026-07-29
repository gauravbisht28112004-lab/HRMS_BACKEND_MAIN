package com.financebuddha.finbud.hrms.entity;

import com.financebuddha.finbud.hrms.entity.base.BaseEntity;
import com.financebuddha.finbud.hrms.enums.SalaryStructureType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "salary_structures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SalaryStructure extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    /**
     * @deprecated Legacy component field. New CTC-based salary structures should use
     * {@link #monthlyGrossCtc} / {@link #nth}. Retained nullable for backward compatibility.
     */
    @Deprecated
    @Column(name = "basic_salary", precision = 12, scale = 2)
    private BigDecimal basicSalary;

    /**
     * @deprecated Legacy component field. See {@link #basicSalary}.
     */
    @Deprecated
    @Column(name = "hra", precision = 12, scale = 2)
    private BigDecimal hra;

    @Column(name = "da", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal da = BigDecimal.ZERO;

    @Column(name = "conveyance_allowance", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal conveyanceAllowance = BigDecimal.ZERO;

    @Column(name = "medical_allowance", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal medicalAllowance = BigDecimal.ZERO;

    @Column(name = "special_allowance", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal specialAllowance = BigDecimal.ZERO;

    @Column(name = "pf_employee_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal pfEmployeePercentage = new BigDecimal("12.00");

    @Column(name = "pf_employer_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal pfEmployerPercentage = new BigDecimal("12.00");

    @Column(name = "esi_employee_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal esiEmployeePercentage = new BigDecimal("0.75");

    @Column(name = "esi_employer_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal esiEmployerPercentage = new BigDecimal("3.25");

    @Column(name = "professional_tax_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal professionalTaxAmount = new BigDecimal("200.00");

    @Column(name = "annual_ctc", precision = 19, scale = 4)
    private BigDecimal annualCtc;

    // ------------------------------------------------------------------
    // Finbud CTC / NTH model (V4)
    // ------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "structure_type", length = 30)
    private SalaryStructureType structureType;

    @Column(name = "monthly_gross_ctc", precision = 19, scale = 4)
    private BigDecimal monthlyGrossCtc;

    @Column(name = "nth", precision = 19, scale = 4)
    private BigDecimal nth;

    @Column(name = "tds_amount", precision = 19, scale = 4)
    private BigDecimal tdsAmount;

    @Column(name = "tds_rate_percent", precision = 5, scale = 2)
    private BigDecimal tdsRatePercent;

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

    @Column(name = "incentives", precision = 19, scale = 4)
    private BigDecimal incentives;

    @Column(name = "other_deductions", precision = 19, scale = 4)
    private BigDecimal otherDeductions;

    @Column(name = "num_of_months")
    private Integer numOfMonths;

    // ------------------------------------------------------------------
    // Validity window
    // ------------------------------------------------------------------

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    public BigDecimal getMonthlyCtc() {
        return annualCtc != null ? annualCtc.divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    /**
     * @deprecated Legacy component-based PF calculation. Use {@link #employeePf} directly
     *             for CTC-model structures (Finbud: fixed INR 1950 for MANAGEMENT / HIGHLY_SKILLED).
     */
    @Deprecated
    public BigDecimal getPfEmployeeContribution() {
        if (basicSalary == null || pfEmployeePercentage == null) return BigDecimal.ZERO;
        return basicSalary.multiply(pfEmployeePercentage).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
    }

    /**
     * @deprecated See {@link #getPfEmployeeContribution()}.
     */
    @Deprecated
    public BigDecimal getPfEmployerContribution() {
        if (basicSalary == null || pfEmployerPercentage == null) return BigDecimal.ZERO;
        return basicSalary.multiply(pfEmployerPercentage).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
    }

    /**
     * @deprecated Legacy ESI calculation. Use {@link #employeeEsi} directly for CTC-model structures.
     */
    @Deprecated
    public BigDecimal getEsiEmployeeContribution(BigDecimal grossSalary) {
        if (grossSalary == null || esiEmployeePercentage == null) return BigDecimal.ZERO;
        if (grossSalary.compareTo(new BigDecimal("21000")) <= 0) {
            return grossSalary.multiply(esiEmployeePercentage).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /**
     * @deprecated See {@link #getEsiEmployeeContribution(BigDecimal)}.
     */
    @Deprecated
    public BigDecimal getEsiEmployerContribution(BigDecimal grossSalary) {
        if (grossSalary == null || esiEmployerPercentage == null) return BigDecimal.ZERO;
        if (grossSalary.compareTo(new BigDecimal("21000")) <= 0) {
            return grossSalary.multiply(esiEmployerPercentage).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}
