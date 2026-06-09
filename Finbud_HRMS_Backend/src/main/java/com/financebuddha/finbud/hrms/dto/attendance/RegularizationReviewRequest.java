package com.financebuddha.finbud.hrms.dto.attendance;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Approve / reject action on a regularization request. */
@Data
public class RegularizationReviewRequest {

    @NotNull(message = "approve flag is required")
    private Boolean approve;

    private String reviewNotes;
}
