package com.financebuddha.finbud.hrms.dto.shift;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Data
public class ShiftTypeRequest {

    @NotBlank(message = "Shift name is required")
    private String name;

    @NotBlank(message = "Shift code is required")
    private String code;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    private Integer breakDurationMinutes = 60;
    private Integer gracePeriodMinutes = 10;
    private List<Integer> weeklyOffDays;
    private Boolean isNightShift = false;
    private BigDecimal overtimeThresholdHours = new BigDecimal("8.00");
}
