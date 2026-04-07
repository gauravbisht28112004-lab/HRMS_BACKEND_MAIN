package com.financebuddha.finbud.hrms.dto.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PunchRequest {

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    @NotNull(message = "Timestamp is required")
    private LocalDateTime timestamp;

    private String deviceId;
    private String type; // IN or OUT
    private String location;
}
