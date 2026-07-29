package com.financebuddha.finbud.hrms.dto.sysconfig;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** Admin payload for setting / updating the org-wide monthly disbursal goal. */
@Data
public class OrgMonthlyGoalRequest {

    /** Goal amount in INR. Must be ≥ 0; zero is valid (resets the dashboard tile). */
    @NotNull
    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal amount;
}
