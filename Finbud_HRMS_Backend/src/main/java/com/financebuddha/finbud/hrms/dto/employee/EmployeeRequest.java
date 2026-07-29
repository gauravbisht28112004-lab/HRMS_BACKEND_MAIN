package com.financebuddha.finbud.hrms.dto.employee;

import com.financebuddha.finbud.hrms.enums.BackgroundCheckStatus;
import com.financebuddha.finbud.hrms.enums.BloodGroup;
import com.financebuddha.finbud.hrms.enums.EmployeeCategory;
import com.financebuddha.finbud.hrms.enums.EmployeeStatus;
import com.financebuddha.finbud.hrms.enums.EmploymentType;
import com.financebuddha.finbud.hrms.enums.Gender;
import com.financebuddha.finbud.hrms.enums.MaritalStatus;
import com.financebuddha.finbud.hrms.enums.ProducerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Payload for admin-driven employee create / update.
 * All fields below `dateOfJoining` are optional — the Excel import path writes
 * the full set, whereas an admin may update just a subset through the portal.
 * Note that {@code email} is now optional to accommodate Finbud employees
 * without an official email address (V3 schema change).
 */
@Data
public class EmployeeRequest {

    // ------------------------------------------------------------------
    // Identity
    // ------------------------------------------------------------------

    @NotBlank(message = "First name is required")
    private String firstName;

    private String middleName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String nickName;
    private String fatherName;
    private String spouseName;

    private LocalDate dateOfBirth;
    private Gender gender;
    private MaritalStatus maritalStatus;
    private LocalDate marriageDate;
    private BloodGroup bloodGroup;

    // ------------------------------------------------------------------
    // Contact — email optional (Finbud has employees with no company email)
    // ------------------------------------------------------------------

    @Email(message = "Email should be valid")
    private String email;

    @Email(message = "Personal email should be valid")
    private String personalEmail;

    @Email(message = "Official email should be valid")
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

    private Boolean isPhysicalChallenged;
    private Boolean isInternationalEmployee;

    // ------------------------------------------------------------------
    // Employment
    // ------------------------------------------------------------------

    @NotNull(message = "Date of joining is required")
    @PastOrPresent(message = "Date of joining cannot be in the future")
    private LocalDate dateOfJoining;

    private LocalDate confirmDate;
    private LocalDate dateOfResignation;
    private LocalDate lastWorkingDate;

    private Long departmentId;
    private String designation;
    private Long managerId;

    private EmploymentType employmentType = EmploymentType.FULL_TIME;
    private EmployeeCategory employeeCategory;
    private String employeeSeries;
    private ProducerType producerType;
    private String employeeReferenceNumber;
    private String costCenter;
    private String division;
    private String grade;
    private String teamName;
    private String managerNameText;
    private String branchHead;
    private String unitHead;
    private Integer probationPeriodDays;
    private Integer noticePeriodDays;

    private Long shiftTypeId;
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    // ------------------------------------------------------------------
    // Device / login
    // ------------------------------------------------------------------

    private Integer empCodeOnDevice;
    private String loginUsername;

    // ------------------------------------------------------------------
    // Emergency contact
    // ------------------------------------------------------------------

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    // ------------------------------------------------------------------
    // Background verification
    // ------------------------------------------------------------------

    private BackgroundCheckStatus backgroundCheckStatus;
    private LocalDate backgroundVerificationDate;
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

    private Boolean pfEligible;
    private String pfNumber;
    private String pfScheme;
    private LocalDate pfJoiningDate;
    private Boolean excessEpfEligible;
    private Boolean excessEpsEligible;
    private Boolean existingPfMember;

    private Boolean esiEligible;
    private String esiNumber;

    private Boolean lwfEligible;

    // ------------------------------------------------------------------
    // Misc operational
    // ------------------------------------------------------------------

    private String targetInfo;
    private String employeeRemarks;
    private String offerLetterIssued;
    private String idCardStatus;
    private String punchingStatus;

    @Size(max = 500, message = "Profile picture URL must not exceed 500 characters")
    private String profilePictureUrl;
}
