package com.financebuddha.finbud.hrms.dto.shift;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for a shift assignment. Embeds enough shift-type context
 * (code + name) so the frontend timeline doesn't need a second lookup.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftAssignmentResponse {

    private Long id;

    private Long employeeId;
    private String employeeCode;
    private String employeeName;

    private Long shiftTypeId;
    private String shiftTypeCode;
    private String shiftTypeName;

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    /** true when effective_to is null or >= today. */
    private Boolean current;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
