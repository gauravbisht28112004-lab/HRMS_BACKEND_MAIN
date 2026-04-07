package com.financebuddha.finbud.hrms.dto.salary;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SalaryStructureRequest {

    @NotNull(message = "Basic salary is required")
    @Positive(message = "Basic salary must be positive")
    private BigDecimal basicSalary;

    @NotNull(message = "HRA is required")
    @Positive(message = "HRA must be positive")
    private BigDecimal hra;

    private BigDecimal da;
    private BigDecimal conveyanceAllowance;
    private BigDecimal medicalAllowance;
    private BigDecimal specialAllowance;

    private BigDecimal pfEmployeePercentage = new BigDecimal("12.00");
    private BigDecimal pfEmployerPercentage = new BigDecimal("12.00");
    private BigDecimal esiEmployeePercentage = new BigDecimal("0.75");
    private BigDecimal esiEmployerPercentage = new BigDecimal("3.25");
    private BigDecimal professionalTaxAmount = new BigDecimal("200.00");

    @NotNull(message = "Annual CTC is required")
    @Positive(message = "Annual CTC must be positive")
    private BigDecimal annualCtc;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
    private Boolean isActive = true;
}
