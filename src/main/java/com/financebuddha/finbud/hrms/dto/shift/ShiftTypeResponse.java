package com.financebuddha.finbud.hrms.dto.shift;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftTypeResponse {

    private Long id;
    private String name;
    private String code;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer breakDurationMinutes;
    private Integer gracePeriodMinutes;
    private List<Integer> weeklyOffDays;
    private Boolean isNightShift;
    private BigDecimal overtimeThresholdHours;
    private LocalDateTime createdAt;
}
