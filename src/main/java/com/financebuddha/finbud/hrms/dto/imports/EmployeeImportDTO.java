package com.financebuddha.finbud.hrms.dto.imports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Raw row shape parsed from a Finbud employee-master Excel file.
 * <p>
 * Every field is a string because Excel rarely gives us clean typed cells —
 * date parsing, empty-string-to-null normalisation, and enum mapping all
 * happen inside {@code EmployeeImportService}. Fields are named after the
 * destination entity fields (not the raw Excel header), which keeps the
 * {@code ExcelRowMapper} header-normalisation layer clean.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeImportDTO {

    // ------------------------------------------------------------------
    // Identity
    // ------------------------------------------------------------------
    private String employeeCode;           // e.g. "ND33004" — mapped to employee_id
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;               // fallback when firstName/lastName absent
    private String nickName;
    private String fatherName;
    private String spouseName;
    private String dateOfBirth;            // parsed to LocalDate in service
    private String gender;
    private String maritalStatus;
    private String marriageDate;
    private String bloodGroup;

    // ------------------------------------------------------------------
    // Contact
    // ------------------------------------------------------------------
    private String email;
    private String personalEmail;
    private String officialEmail;
    private String phone;
    private String mobileNumber;
    private String extensionNumber;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String countryOfOrigin;
    private String location;
    private String isPhysicalChallenged;
    private String isInternationalEmployee;

    // ------------------------------------------------------------------
    // Employment
    // ------------------------------------------------------------------
    private String dateOfJoining;
    private String confirmDate;
    private String dateOfResignation;
    private String lastWorkingDate;
    private String department;
    private String designation;
    private String reportingManagerCode;   // links to another row's employeeCode
    private String managerNameText;        // free-text manager label from Excel
    private String employmentType;         // "Full Time" / "Contract" / ...
    private String employeeCategory;       // Permanent / Contract / Intern / ...
    private String employeeSeries;
    private String producerType;           // Producer / Non-Producer
    private String employeeReferenceNumber;
    private String costCenter;
    private String division;
    private String grade;
    private String teamName;
    private String branchHead;
    private String unitHead;
    private String probationPeriodDays;
    private String noticePeriodDays;
    private String status;                 // Active / Inactive / Resigned / ...

    // ------------------------------------------------------------------
    // Device / login
    // ------------------------------------------------------------------
    private String empCodeOnDevice;        // optional — defaults to last 5 digits of employeeCode
    private String loginUsername;          // optional — defaults to employeeCode

    // ------------------------------------------------------------------
    // Emergency contact
    // ------------------------------------------------------------------
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    // ------------------------------------------------------------------
    // Background verification
    // ------------------------------------------------------------------
    private String backgroundCheckStatus;
    private String backgroundVerificationDate;
    private String backgroundAgencyName;
    private String backgroundCheckRemarks;

    // ------------------------------------------------------------------
    // Banking
    // ------------------------------------------------------------------
    private String bankAccountNumber;
    private String bankIfscCode;
    private String bankName;
    private String bankAccountType;
    private String bankBranch;
    private String salaryPaymentMode;
    private String ddPayableAt;
    private String nameAsPerBank;
    private String iban;

    // ------------------------------------------------------------------
    // Statutory
    // ------------------------------------------------------------------
    private String panNumber;
    private String aadhaarNumber;
    private String aadhaarEnrolmentNo;
    private String aadhaarName;
    private String uanNumber;
    private String pfEligible;
    private String pfNumber;
    private String pfScheme;
    private String pfJoiningDate;
    private String excessEpfEligible;
    private String excessEpsEligible;
    private String existingPfMember;
    private String esiEligible;
    private String esiNumber;
    private String lwfEligible;

    // ------------------------------------------------------------------
    // Salary — CTC / NTH model
    // ------------------------------------------------------------------
    private String structureType;          // CONTRACT / MANAGEMENT / HIGHLY_SKILLED
    private String monthlyGrossCtc;
    private String nth;
    private String annualCtc;
    private String tdsAmount;
    private String tdsRatePercent;
    private String employerPf;
    private String employeePf;
    private String employerEsi;
    private String employeeEsi;
    private String lwfAmount;
    private String incentives;
    private String otherDeductions;
    private String numOfMonths;

    // ------------------------------------------------------------------
    // Misc
    // ------------------------------------------------------------------
    private String targetInfo;
    private String employeeRemarks;
    private String offerLetterIssued;
    private String idCardStatus;
    private String punchingStatus;

    // ------------------------------------------------------------------
    // Debugging
    // ------------------------------------------------------------------
    private Integer rowNumber;
    private String rawData;
}
