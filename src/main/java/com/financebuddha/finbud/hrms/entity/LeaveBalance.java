package com.financebuddha.finbud.hrms.entity;

import com.financebuddha.finbud.hrms.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "leave_balances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LeaveBalance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "casual_leave_allocated", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal casualLeaveAllocated = new BigDecimal("12.00");

    @Column(name = "casual_leave_used", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal casualLeaveUsed = BigDecimal.ZERO;

    @Column(name = "sick_leave_allocated", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal sickLeaveAllocated = new BigDecimal("12.00");

    @Column(name = "sick_leave_used", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal sickLeaveUsed = BigDecimal.ZERO;

    @Column(name = "paid_leave_allocated", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal paidLeaveAllocated = BigDecimal.ZERO;

    @Column(name = "paid_leave_used", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal paidLeaveUsed = BigDecimal.ZERO;

    @Column(name = "wfh_days_allocated")
    @Builder.Default
    private Integer wfhDaysAllocated = 12;

    @Column(name = "wfh_days_used")
    @Builder.Default
    private Integer wfhDaysUsed = 0;

    @Column(name = "lop_days", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal lopDays = BigDecimal.ZERO;

    public BigDecimal getCasualLeaveBalance() {
        return casualLeaveAllocated.subtract(casualLeaveUsed);
    }

    public BigDecimal getSickLeaveBalance() {
        return sickLeaveAllocated.subtract(sickLeaveUsed);
    }

    public BigDecimal getPaidLeaveBalance() {
        return paidLeaveAllocated.subtract(paidLeaveUsed);
    }

    public Integer getWfhBalance() {
        return wfhDaysAllocated - wfhDaysUsed;
    }
}
