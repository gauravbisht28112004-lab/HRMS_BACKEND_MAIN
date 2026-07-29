package com.financebuddha.finbud.hrms.dto.employee;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload for {@code POST /api/employees}.
 * <p>
 * Wraps the usual {@link EmployeeResponse} with the credentials that were
 * auto-provisioned for the new employee. The plaintext {@code temporaryPassword}
 * is deliberately returned here — it is the <b>only time</b> the caller will
 * ever see it. The UI is expected to surface it in a copy-once modal and then
 * discard it. It is never persisted in logs or stored elsewhere.
 * <p>
 * When user provisioning is skipped (e.g. a User row already exists for the
 * employee, or provisioning failed for a non-fatal reason), {@code userProvisioned}
 * is {@code false} and the credential fields are {@code null}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeCreateResponse {

    /** The freshly-created employee record. */
    private EmployeeResponse employee;

    /** Whether a User account was auto-provisioned alongside the employee row. */
    private Boolean userProvisioned;

    /**
     * The generated username for the new login. Derived from
     * {@code employee.loginUsername} if supplied, otherwise
     * {@code employeeId.toLowerCase()}.
     */
    private String generatedUsername;

    /**
     * The plaintext temporary password — one-time leak. The UI must show this
     * exactly once and rely on {@code mustChangePassword=true} on first login
     * to force a rotation.
     */
    private String generatedTemporaryPassword;

    /**
     * Human-readable note describing why provisioning was skipped, when
     * {@link #userProvisioned} is {@code false}. Null on the happy path.
     */
    private String provisioningSkippedReason;
}
