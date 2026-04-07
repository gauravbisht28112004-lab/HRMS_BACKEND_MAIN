package com.financebuddha.finbud.hrms.dto.leave;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeaveApprovalRequest {

    @NotNull(message = "Approval status is required")
    private Boolean approved;

    private String rejectionReason;
}
