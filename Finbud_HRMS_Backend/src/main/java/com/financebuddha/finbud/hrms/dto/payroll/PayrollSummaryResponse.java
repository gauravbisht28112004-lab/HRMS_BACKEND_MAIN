package com.financebuddha.finbud.hrms.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollSummaryResponse {

    private Integer month;
    private Integer year;
    private Long totalEmployees;
    private BigDecimal totalGrossEarnings;
    private BigDecimal totalDeductions;
    private BigDecimal totalNetPay;
    private BigDecimal totalOvertimePay;
    private Long paidCount;
    private Long pendingCount;
}
