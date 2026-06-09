package com.financebuddha.finbud.hrms.dto.attendance;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Approval / rejection action taken by a TL/HR/Admin on a single
 * attendance row. Rejection requires a reason so the employee has
 * context when they file a regularization.
 */
@Data
public class AttendanceApprovalRequest {

    @NotNull(message = "approve flag is required")
    private Boolean approve;

    private String rejectionReason;
}
