package com.financebuddha.finbud.hrms.dto.employee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    public static class SalaryInfo {
        private String salaryStructureId;
        private String annualCtc;
        private String monthlyCtc;
    }

    @Data
    @Builder
    public static class BankInfo {
        private String accountNumber;
        private String ifscCode;
        private String bankName;
    }

    @Data
    @Builder
    public static class IdentityInfo {
        private String panNumber;
        private String aadhaarNumber;
    }
}
