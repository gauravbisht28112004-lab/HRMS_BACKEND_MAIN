package com.financebuddha.finbud.hrms.dto.department;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponse {

    private Long id;
    private String name;
    private String code;
    private String description;
    private Long managerId;
    private String managerName;
    private Long employeeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
