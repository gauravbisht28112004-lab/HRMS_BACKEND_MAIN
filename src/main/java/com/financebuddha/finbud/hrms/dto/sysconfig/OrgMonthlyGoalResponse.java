package com.financebuddha.finbud.hrms.dto.sysconfig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Wire shape of the org-wide monthly disbursal goal. Read by every
 * dashboard, set by Admin only.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgMonthlyGoalResponse {

    /** Goal amount in INR. May be {@code 0} if Admin hasn't set it yet. */
    private BigDecimal amount;

    /** Currency hint for the UI. Always "INR" today. */
    private String currency;
}
