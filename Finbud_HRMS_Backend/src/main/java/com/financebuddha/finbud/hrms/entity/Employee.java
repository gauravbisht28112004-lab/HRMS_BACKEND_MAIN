package com.financebuddha.finbud.hrms.entity;

import com.financebuddha.finbud.hrms.entity.base.BaseEntity;
import com.financebuddha.finbud.hrms.enums.BackgroundCheckStatus;
import com.financebuddha.finbud.hrms.enums.BloodGroup;
import com.financebuddha.finbud.hrms.enums.EmployeeCategory;
import com.financebuddha.finbud.hrms.enums.EmployeeStatus;
import com.financebuddha.finbud.hrms.enums.EmploymentType;
import com.financebuddha.finbud.hrms.enums.Gender;
import com.financebuddha.finbud.hrms.enums.MaritalStatus;
import com.financebuddha.finbud.hrms.enums.ProducerType;
import com.financebuddha.finbud.hrms.converter.GenderConverter;
import com.financebuddha.finbud.hrms.converter.MaritalStatusConverter;
import com.financebuddha.finbud.hrms.converter.BloodGroupConverter;
import com.financebuddha.finbud.hrms.converter.EmploymentTypeConverter;
import com.financebuddha.finbud.hrms.converter.EmployeeCategoryConverter;
import com.financebuddha.finbud.hrms.converter.ProducerTypeConverter;
import com.financebuddha.finbud.hrms.converter.BackgroundCheckStatusConverter;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Employee extends BaseEntity {

    // ------------------------------------------------------------------
    // Identity
    // ------------------------------------------------------------------

    @Column(name = "employee_id", nullable = false, unique = true, length = 20)
    private String employeeId;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "nick_name", length = 100)
    private String nickName;

    @Column(name = "father_name", length = 150)
    private String fatherName;

    @Column(name = "spouse_name", length = 150)
    private String spouseName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Convert(converter = GenderConverter.class)
    @Column(name = "gender", length = 30)
    private Gender gender;

    @Convert(converter = MaritalStatusConverter.class)
    @Column(name = "marital_status", length = 20)
    private MaritalStatus maritalStatus;

    @Column(name = "marriage_date")
    private LocalDate marriageDate;

    @Convert(converter = BloodGroupConverter.class)
    @Column(name = "blood_group", length = 20)
    private BloodGroup bloodGroup;

    // ------------------------------------------------------------------
    // Contact
    // ------------------------------------------------------------------

    // Note: email is now nullable (V3) to accommodate employees without an official email.
    // Unique constraint retained; PostgreSQL treats multiple NULLs as distinct.
    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Column(name = "personal_email", length = 150)
    private String personalEmail;

    @Column(name = "official_email", length = 150)
    private String officialEmail;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;

    @Column(name = "extension_number", length = 20)
    private String extensionNumber;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 50)
    private String city;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "country_of_origin", length = 50)
    private String countryOfOrigin;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "is_physical_challenged")
    @Builder.Default
    private Boolean isPhysicalChallenged = false;

    @Column(name = "is_international_employee")
    @Builder.Default
    private Boolean isInternationalEmployee = false;

    // ------------------------------------------------------------------
    // Employment
    // ------------------------------------------------------------------

    @Column(name = "date_of_joining", nullable = false)
    private LocalDate dateOfJoining;

    @Column(name = "confirm_date")
    private LocalDate confirmDate;

    @Column(name = "date_of_resignation")
    private LocalDate dateOfResignation;

    @Column(name = "last_working_date")
    private LocalDate lastWorkingDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "designation", length = 100)
    private String designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @OneToMany(mappedBy = "manager", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Employee> subordinates = new ArrayList<>();

    @Convert(converter = EmploymentTypeConverter.class)
    @Column(name = "employment_type", length = 20)
    @Builder.Default
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    @Convert(converter = EmployeeCategoryConverter.class)
    @Column(name = "employee_category", length = 30)
    private EmployeeCategory employeeCategory;

    @Column(name = "employee_series", length = 50)
    private String employeeSeries;

    @Convert(converter = ProducerTypeConverter.class)
    @Column(name = "producer_type", length = 30)
    private ProducerType producerType;

    @Column(name = "employee_reference_number", length = 50)
    private String employeeReferenceNumber;

    @Column(name = "cost_center", length = 100)
    private String costCenter;

    @Column(name = "division", length = 100)
    private String division;

    @Column(name = "grade", length = 50)
    private String grade;

    @Column(name = "team_name", length = 100)
    private String teamName;

    @Column(name = "manager_name_text", length = 150)
    private String managerNameText;

    @Column(name = "branch_head", length = 150)
    private String branchHead;

    @Column(name = "unit_head", length = 150)
    private String unitHead;

    @Column(name = "probation_period_days")
    private Integer probationPeriodDays;

    @Column(name = "notice_period_days")
    private Integer noticePeriodDays;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_type_id")
    private ShiftType shiftType;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ShiftAssignment> shiftAssignments = new ArrayList<>();

    // Office the employee reports to. Used to enforce punch-geofence when
    // OfficeLocation.enforceGeofence is true. Optional — new employees pick
    // up the seeded default office via the V11 backfill.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "office_location_id")
    private OfficeLocation officeLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    // ------------------------------------------------------------------
    // Device / login
    // ------------------------------------------------------------------

    @Column(name = "emp_code_on_device", unique = true)
    private Integer empCodeOnDevice;

    @Column(name = "login_username", unique = true, length = 50)
    private String loginUsername;

    // ------------------------------------------------------------------
    // Emergency contact
    // ------------------------------------------------------------------

    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relationship", length = 50)
    private String emergencyContactRelationship;

    // ------------------------------------------------------------------
    // Background verification
    // ------------------------------------------------------------------

    @Convert(converter = BackgroundCheckStatusConverter.class)
    @Column(name = "background_check_status", length = 30)
    private BackgroundCheckStatus backgroundCheckStatus;

    @Column(name = "background_verification_date")
    private LocalDate backgroundVerificationDate;

    @Column(name = "background_agency_name", length = 150)
    private String backgroundAgencyName;

    @Column(name = "background_check_remarks", columnDefinition = "TEXT")
    private String backgroundCheckRemarks;

    // ------------------------------------------------------------------
    // Banking
    // ------------------------------------------------------------------

    @Column(name = "bank_account_number", length = 30)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc_code", length = 20)
    private String bankIfscCode;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_type", length = 10)
    private String bankAccountType;

    @Column(name = "bank_branch", length = 150)
    private String bankBranch;

    @Column(name = "salary_payment_mode", length = 30)
    private String salaryPaymentMode;

    @Column(name = "dd_payable_at", length = 150)
    private String ddPayableAt;

    @Column(name = "name_as_per_bank", length = 150)
    private String nameAsPerBank;

    @Column(name = "iban", length = 50)
    private String iban;

    // ------------------------------------------------------------------
    // Statutory (PF / ESI / LWF / Aadhaar / UAN / PAN)
    // ------------------------------------------------------------------

    @Column(name = "pan_number", length = 10)
    private String panNumber;

    @Column(name = "aadhaar_number", length = 12)
    private String aadhaarNumber;

    @Column(name = "aadhaar_enrolment_no", length = 30)
    private String aadhaarEnrolmentNo;

    @Column(name = "aadhaar_name", length = 150)
    private String aadhaarName;

    @Column(name = "uan_number", length = 20)
    private String uanNumber;

    @Column(name = "pf_eligible")
    @Builder.Default
    private Boolean pfEligible = false;

    @Column(name = "pf_number", length = 50)
    private String pfNumber;

    @Column(name = "pf_scheme", length = 50)
    private String pfScheme;

    @Column(name = "pf_joining_date")
    private LocalDate pfJoiningDate;

    @Column(name = "excess_epf_eligible")
    @Builder.Default
    private Boolean excessEpfEligible = false;

    @Column(name = "excess_eps_eligible")
    @Builder.Default
    private Boolean excessEpsEligible = false;

    @Column(name = "existing_pf_member")
    @Builder.Default
    private Boolean existingPfMember = false;

    @Column(name = "esi_eligible")
    @Builder.Default
    private Boolean esiEligible = false;

    @Column(name = "esi_number", length = 30)
    private String esiNumber;

    @Column(name = "lwf_eligible")
    @Builder.Default
    private Boolean lwfEligible = false;

    // ------------------------------------------------------------------
    // Misc. operational
    // ------------------------------------------------------------------

    @Column(name = "target_info", length = 255)
    private String targetInfo;

    @Column(name = "employee_remarks", columnDefinition = "TEXT")
    private String employeeRemarks;

    @Column(name = "offer_letter_issued", length = 10)
    private String offerLetterIssued;

    @Column(name = "id_card_status", length = 10)
    private String idCardStatus;

    @Column(name = "punching_status", length = 10)
    private String punchingStatus;

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    /**
     * S3 / MinIO object key for the uploaded avatar, e.g.
     * {@code avatars/ND33004/1b8f7...e0.jpg}. Null when the employee has
     * not uploaded a picture — in that case the DTO falls back to
     * {@link #profilePictureUrl} (a legacy pasted URL) if present.
     */
    @Column(name = "avatar_key", length = 512)
    private String avatarKey;

    /** MIME type recorded at upload time ({@code image/jpeg} etc.). */
    @Column(name = "avatar_content_type", length = 100)
    private String avatarContentType;

    // ------------------------------------------------------------------
    // Relationships
    // ------------------------------------------------------------------

    // NOTE: stays @OneToOne for now. If Finbud adopts multi-row salary history
    // (H5), this needs to flip to @OneToMany together with a SalaryServiceImpl
    // refactor — will surface that change in Checkpoint 5 before making it.
    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private SalaryStructure salaryStructure;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private User user;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Attendance> attendances = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    @Builder.Default
    private List<LeaveRequest> leaveRequests = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Payroll> payrolls = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    @Builder.Default
    private List<LeaveBalance> leaveBalances = new ArrayList<>();

    // ------------------------------------------------------------------
    // Derived
    // ------------------------------------------------------------------

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null) sb.append(firstName);
        if (middleName != null && !middleName.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(middleName);
        }
        if (lastName != null && !lastName.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(lastName);
        }
        return sb.toString();
    }
}
