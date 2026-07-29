package com.financebuddha.finbud.hrms.dto.commitment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * TL / HR / Admin reviews a SUBMITTED daily commitment. {@code approve=true}
 * → status APPROVED. {@code approve=false} → status REJECTED, in which case
 * {@link #rejectionReason} is required so the employee knows what to fix.
 */
@Data
public class DailyCommitmentReviewRequest {

    @NotNull
    private Boolean approve;

    @Size(min = 3, max = 500)
    private String rejectionReason;
}
