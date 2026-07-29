package com.financebuddha.finbud.hrms.dto.employee;

import com.financebuddha.finbud.hrms.enums.BackgroundCheckStatus;
import com.financebuddha.finbud.hrms.enums.BloodGroup;
import com.financebuddha.finbud.hrms.enums.EmployeeCategory;
import com.financebuddha.finbud.hrms.enums.EmployeeStatus;
import com.financebuddha.finbud.hrms.enums.EmploymentType;
import com.financebuddha.finbud.hrms.enums.Gender;
import com.financebuddha.finbud.hrms.enums.MaritalStatus;
import com.financebuddha.finbud.hrms.enums.ProducerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {

    private Long id;
    private String employeeId;

    // ------------------------------------------------------------------
    // Identity
    // ------------------------------------------------------------------
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private String nickName;
    private String fatherName;
    private String spouseName;

    private LocalDate dateOfBirth;
    private Gender gender;
    private MaritalStatus maritalStatus;
    private LocalDate marriageDate;
    private BloodGroup bloodGroup;

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

    private Boolean isPhysicalChallenged;
    private Boolean isInternationalEmployee;

    // ------------------------------------------------------------------
    // Employment
    // ------------------------------------------------------------------
    private LocalDate dateOfJoining;
    private LocalDate confirmDate;
    private LocalDate dateOfResignation;
    private LocalDate lastWorkingDate;

    private Long departmentId;
    private String departmentName;

    private String designation;

    private Long managerId;
    private String managerName;

    private EmploymentType employmentType;
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

    private EmployeeStatus status;

    private Long shiftTypeId;
    private String shiftName;

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
    // Statutory flags (full statutory numbers live in EmployeeDetailResponse)
    // ------------------------------------------------------------------
    private Boolean pfEligible;
    private Boolean esiEligible;
    private Boolean lwfEligible;

    // ------------------------------------------------------------------
    // Misc operational
    // ------------------------------------------------------------------
    private String targetInfo;
    private String employeeRemarks;
    private String offerLetterIssued;
    private String idCardStatus;
    private String punchingStatus;
    private String profilePictureUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
