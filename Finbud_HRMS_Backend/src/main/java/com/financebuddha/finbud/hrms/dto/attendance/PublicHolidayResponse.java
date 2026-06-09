package com.financebuddha.finbud.hrms.dto.attendance;

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
public class PublicHolidayResponse {

    private Long id;
    private LocalDate holidayDate;
    private String name;
    private String description;
    private Boolean isOptional;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
