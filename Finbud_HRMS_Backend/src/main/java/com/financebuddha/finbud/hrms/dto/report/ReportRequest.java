package com.financebuddha.finbud.hrms.dto.report;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReportRequest {

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private Long departmentId;
    private Long employeeId;
    private String exportFormat = "PDF"; // PDF, EXCEL, CSV
}
