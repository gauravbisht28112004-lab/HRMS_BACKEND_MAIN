package com.financebuddha.finbud.hrms.dto.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Employee-initiated request to correct their attendance for one date.
 */
@Data
public class RegularizationRequestDto {

    @NotNull(message = "Attendance date is required")
    private LocalDate attendanceDate;

    private LocalDateTime requestedPunchIn;

    private LocalDateTime requestedPunchOut;

    @NotBlank(message = "Reason is required")
    private String reason;
}
