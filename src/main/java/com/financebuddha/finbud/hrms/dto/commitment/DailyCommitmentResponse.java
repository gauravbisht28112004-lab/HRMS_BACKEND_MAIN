package com.financebuddha.finbud.hrms.dto.commitment;

import com.financebuddha.finbud.hrms.enums.CommitmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Wire shape for one daily-commitment row. Includes employee + approver
 * full names so the UI can render "Akash's commitment, approved by Anjali"
 * without a second lookup.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyCommitmentResponse {

    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;

    private LocalDate workDate;

    // Targets
    private Integer targetCalls;
    private Integer targetOtps;
    private Integer targetInterestedCustomers;
    private BigDecimal targetDisbursalAmount;

    // Actuals
    private Integer actualCalls;
    private Integer actualOtps;
    private Integer actualInterestedCustomers;
    private BigDecimal actualDisbursalAmount;

    // Workflow
    private CommitmentStatus status;
    private LocalDateTime submittedAt;
    private Long approvedById;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
