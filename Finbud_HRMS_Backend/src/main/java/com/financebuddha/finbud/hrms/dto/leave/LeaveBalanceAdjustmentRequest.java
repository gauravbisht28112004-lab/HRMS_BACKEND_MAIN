package com.financebuddha.finbud.hrms.dto.leave;

import com.financebuddha.finbud.hrms.enums.LeaveBalanceBucket;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payload for the HR-only balance adjust endpoint. A single atomic bump to
 * one bucket of one employee's balance for a given year.
 *
 * <p>{@link #delta} may be positive (grant days — e.g. comp-off) or negative
 * (claw back days — e.g. correct a double-allocation). Zero is rejected.
 * The service clamps the resulting field at zero so a mis-entered negative
 * can't corrupt the row.
 *
 * <p>{@link #reason} is mandatory. It goes into the audit log so any future
 * question "why does Akash have 7 paid leaves?" is self-service to answer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceAdjustmentRequest {

    /** Calendar year whose balance is being adjusted. */
    @NotNull
    private Integer year;

    /** Which bucket of the balance to touch. */
    @NotNull
    private LeaveBalanceBucket bucket;

    /**
     * Signed change in days. Positive adds capacity (grants leave),
     * negative subtracts. Zero is invalid.
     */
    @NotNull
    private BigDecimal delta;

    @NotNull
    @Size(min = 3, max = 500)
    private String reason;
}
