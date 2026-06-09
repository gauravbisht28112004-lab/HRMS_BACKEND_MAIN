package com.financebuddha.finbud.hrms.dto.leave;

import com.financebuddha.finbud.hrms.enums.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for the HR / Admin override endpoint.
 *
 * <p>Unlike {@code LeaveApprovalRequest}, this can flip an *already decided*
 * leave to any new status. The legal target states are:
 * <ul>
 *   <li>{@link LeaveStatus#APPROVED} — flip to approved (re-deducts balance
 *       if previously rejected/cancelled).</li>
 *   <li>{@link LeaveStatus#REJECTED} — flip to rejected (restores balance
 *       if previously approved).</li>
 *   <li>{@link LeaveStatus#CANCELLED} — HR cancels on the employee's
 *       behalf (restores balance if previously approved).</li>
 * </ul>
 * Any other target value is rejected by the service with a 400.
 *
 * <p>{@code reason} is mandatory and goes into the {@code audit_logs} row
 * so any "why did Akash's leave change to rejected?" question is
 * self-service.
 */
@Data
public class LeaveOverrideRequest {

    @NotNull
    private LeaveStatus targetStatus;

    @NotNull
    @Size(min = 3, max = 500)
    private String reason;
}
