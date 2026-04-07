package com.financebuddha.finbud.hrms.dto.payroll;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PayrollGenerateRequest {

    @NotNull(message = "Month is required")
    @Min(1)
    @Max(12)
    private Integer month;

    @NotNull(message = "Year is required")
    @Min(2000)
    private Integer year;

    private Long employeeId; // If null, generate for all employees
}
