package com.financebuddha.finbud.hrms.dto.employee;

import com.financebuddha.finbud.hrms.enums.EmployeeStatus;
import com.financebuddha.finbud.hrms.enums.EmploymentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    private String phone;
    private String address;
    private String city;
    private String state;
    private String pincode;

    @NotNull(message = "Date of joining is required")
    @PastOrPresent(message = "Date of joining cannot be in the future")
    private LocalDate dateOfJoining;

    private Long departmentId;
    private String designation;
    private Long managerId;

    private EmploymentType employmentType = EmploymentType.FULL_TIME;
    private Long shiftTypeId;
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    private String bankAccountNumber;
    private String bankIfscCode;
    private String bankName;

    private String panNumber;
    private String aadhaarNumber;
    private String profilePictureUrl;
}
