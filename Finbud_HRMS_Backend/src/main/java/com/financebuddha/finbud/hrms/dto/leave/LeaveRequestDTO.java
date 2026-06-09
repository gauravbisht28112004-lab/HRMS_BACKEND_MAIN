package com.financebuddha.finbud.hrms.dto.leave;

import com.financebuddha.finbud.hrms.enums.HalfDayType;
import com.financebuddha.finbud.hrms.enums.LeaveType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveRequestDTO {

    @NotNull(message = "Leave type is required")
    private LeaveType leaveType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String contactDuringLeave;
    private Boolean isHalfDay = false;
    private HalfDayType halfDayType;
}
