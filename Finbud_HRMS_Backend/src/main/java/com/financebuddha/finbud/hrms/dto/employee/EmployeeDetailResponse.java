package com.financebuddha.finbud.hrms.dto.employee;

import com.financebuddha.finbud.hrms.enums.SalaryStructureType;
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
    private SalaryInfo salaryInfo;
    private BankInfo bankInfo;
    private IdentityInfo identityInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalaryInfo {
        private Long salaryStructureId;
        private SalaryStructureType structureType;
        private BigDecimal monthlyGrossCtc;
        private BigDecimal nth;
        private BigDecimal annualCtc;
        private BigDecimal monthlyCtc;
        private BigDecimal employerPf;
        private BigDecimal employeePf;
        private BigDecimal employerEsi;
        private BigDecimal employeeEsi;
        private BigDecimal lwfAmount;
        private BigDecimal tdsAmount;
        private BigDecimal tdsRatePercent;
        private BigDecimal incentives;
        private BigDecimal otherDeductions;
        private Integer numOfMonths;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private Boolean isActive;
    }

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
