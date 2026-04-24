package com.financebuddha.finbud.hrms.dto.attendance;

import com.financebuddha.finbud.hrms.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * HR / Admin manual attendance entry. Used when someone forgot to punch,
 * is on site without access to the portal, or HR wants to override the
 * nightly auto-Absent flag.
 *
 * <p>If punchIn/punchOut are supplied we compute working hours; otherwise
 * the row is recorded as the supplied status (useful for marking an
 * approved leave day as ON_LEAVE post-hoc, for instance).</p>
 */
@Data
public class AttendanceManualEntryRequest {

    @NotNull(message = "Employee id is required")
    private Long employeeId;

    @NotNull(message = "Date is required")
    private LocalDate attendanceDate;

    private LocalDateTime punchIn;

    private LocalDateTime punchOut;

    @NotNull(message = "Status is required")
    private AttendanceStatus status;

    private String notes;
}
