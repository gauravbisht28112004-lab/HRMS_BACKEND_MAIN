package com.financebuddha.finbud.hrms.dto.leave;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceResponse {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private Integer year;

    private BigDecimal casualLeaveAllocated;
    private BigDecimal casualLeaveUsed;
    private BigDecimal casualLeaveBalance;

    private BigDecimal sickLeaveAllocated;
    private BigDecimal sickLeaveUsed;
    private BigDecimal sickLeaveBalance;

    private BigDecimal paidLeaveAllocated;
    private BigDecimal paidLeaveUsed;
    private BigDecimal paidLeaveBalance;

    private Integer wfhDaysAllocated;
    private Integer wfhDaysUsed;
    private Integer wfhDaysBalance;

    private BigDecimal lopDays;
}
