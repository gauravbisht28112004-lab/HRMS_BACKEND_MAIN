package com.financebuddha.finbud.hrms.entity;

import com.financebuddha.finbud.hrms.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "salary_structures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryStructure extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false, unique = true)
    private Employee employee;

    @Column(name = "basic_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal basicSalary;

    @Column(name = "hra", nullable = false, precision = 12, scale = 2)
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

    @Column(name = "annual_ctc", nullable = false, precision = 12, scale = 2)
    private BigDecimal annualCtc;

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

    public BigDecimal getPfEmployeeContribution() {
        return basicSalary.multiply(pfEmployeePercentage).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
    }

    public BigDecimal getPfEmployerContribution() {
        return basicSalary.multiply(pfEmployerPercentage).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
    }

    public BigDecimal getEsiEmployeeContribution(BigDecimal grossSalary) {
        if (grossSalary.compareTo(new BigDecimal("21000")) <= 0) {
            return grossSalary.multiply(esiEmployeePercentage).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getEsiEmployerContribution(BigDecimal grossSalary) {
        if (grossSalary.compareTo(new BigDecimal("21000")) <= 0) {
            return grossSalary.multiply(esiEmployerPercentage).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}
