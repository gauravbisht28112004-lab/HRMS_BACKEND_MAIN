package com.financebuddha.finbud.hrms.dto.employee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Full detail view of an employee — aggregates the summary {@link EmployeeResponse}
 * with finance-sensitive sub-sections (salary, banking, statutory identifiers).
 * Only ADMIN / HR should be allowed to fetch this shape; the summary response
 * omits these fields for general listing pages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDetailResponse {

    private EmployeeResponse employee;
    private BankInfo bankInfo;
    private IdentityInfo identityInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankInfo {
        private String accountNumber;
        private String ifscCode;
        private String bankName;
        private String accountType;
        private String branch;
        private String salaryPaymentMode;
        private String ddPayableAt;
        private String nameAsPerBank;
        private String iban;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdentityInfo {
        private String panNumber;
        private String aadhaarNumber;
        private String aadhaarEnrolmentNo;
        private String aadhaarName;
        private String uanNumber;
        private String pfNumber;
        private String pfScheme;
        private LocalDate pfJoiningDate;
        private String esiNumber;
        private Boolean pfEligible;
        private Boolean esiEligible;
        private Boolean lwfEligible;
        private Boolean existingPfMember;
        private Boolean excessEpfEligible;
        private Boolean excessEpsEligible;
    }
}
