package com.financebuddha.finbud.hrms.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response from {@code POST /api/admin/users/provision-missing}.
 *
 * <p>Reports how many employee accounts were created, which usernames
 * were provisioned, and the default password they were given — so HR
 * can broadcast credentials to the new employees.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvisionMissingUsersResponse {

    /** Number of User accounts just created. */
    private int provisionedCount;

    /** Employees that already had a login — skipped. */
    private int alreadyProvisionedCount;

    /** Employees that failed (e.g. username collision). */
    private int failedCount;

    /** Default password assigned to every newly created account. */
    private String defaultPassword;

    /** Usernames that were successfully created. */
    private List<String> provisionedUsernames;

    /** Employee codes that failed provisioning, with a reason each. */
    private List<FailureDetail> failures;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailureDetail {
        private String employeeId;
        private String reason;
    }
}
