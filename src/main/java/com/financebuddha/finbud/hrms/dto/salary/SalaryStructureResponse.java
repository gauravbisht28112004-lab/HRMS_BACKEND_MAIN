package com.financebuddha.finbud.hrms.dto.salary;

import com.financebuddha.finbud.hrms.enums.SalaryStructureType;
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
public class SalaryStructureResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;

    // ------------------------------------------------------------------
    // Finbud CTC / NTH model (primary)
    // ------------------------------------------------------------------
    private SalaryStructureType structureType;
    private BigDecimal monthlyGrossCtc;
    private BigDecimal nth;
    private BigDecimal tdsAmount;
    private BigDecimal tdsRatePercent;
    private BigDecimal employerPf;
    private BigDecimal employeePf;
    private BigDecimal employerEsi;
    private BigDecimal employeeEsi;
    private BigDecimal lwfAmount;
    private BigDecimal incentives;
    private BigDecimal otherDeductions;
    private Integer numOfMonths;

    // ------------------------------------------------------------------
    // Annual / monthly totals
    // ------------------------------------------------------------------
    private BigDecimal annualCtc;
    private BigDecimal monthlyCtc;

    // ------------------------------------------------------------------
    // Legacy component view (populated only for pre-V4 records)
    // ------------------------------------------------------------------
    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal da;
    private BigDecimal conveyanceAllowance;
    private BigDecimal medicalAllowance;
    private BigDecimal specialAllowance;
    private BigDecimal totalAllowances;

    // ------------------------------------------------------------------
    // Validity
    // ------------------------------------------------------------------
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
