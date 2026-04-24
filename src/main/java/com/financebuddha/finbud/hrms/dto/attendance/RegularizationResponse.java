package com.financebuddha.finbud.hrms.dto.attendance;

import com.financebuddha.finbud.hrms.enums.RegularizationStatus;
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
public class RegularizationResponse {

    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private Long attendanceId;
    private LocalDate attendanceDate;
    private LocalDateTime requestedPunchIn;
    private LocalDateTime requestedPunchOut;
    private String reason;
    private RegularizationStatus status;
    private Long reviewedById;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String reviewNotes;
    private LocalDateTime createdAt;
}
