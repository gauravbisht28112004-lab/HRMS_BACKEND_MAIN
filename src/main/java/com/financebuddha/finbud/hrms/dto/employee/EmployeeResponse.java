package com.financebuddha.finbud.hrms.dto.employee;

import com.financebuddha.finbud.hrms.enums.EmployeeStatus;
import com.financebuddha.finbud.hrms.enums.EmploymentType;
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
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private LocalDate dateOfJoining;

    private Long departmentId;
    private String departmentName;

    private String designation;

    private Long managerId;
    private String managerName;

    private EmploymentType employmentType;
    private EmployeeStatus status;

    private Long shiftTypeId;
    private String shiftName;

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    private String profilePictureUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
