package com.financebuddha.finbud.hrms.dto.commitment;

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
public class HourlyUpdateResponse {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private LocalDate workDate;
    private String hourSlot;
    private Integer callsDone;
    private Integer otpsAchieved;
    private Integer interestedCustomers;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
