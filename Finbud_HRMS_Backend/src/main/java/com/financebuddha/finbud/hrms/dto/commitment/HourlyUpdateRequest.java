package com.financebuddha.finbud.hrms.dto.commitment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Upsert payload for an hourly update. If a row already exists for the
 * (employee, work_date, hour_slot) tuple the service updates it in place
 * — keeps the UI simple, no separate "edit" endpoint needed.
 */
@Data
public class HourlyUpdateRequest {

    @NotNull
    private LocalDate workDate;

    @NotBlank
    @Size(max = 15)
    private String hourSlot;

    @NotNull
    @Min(0)
    private Integer callsDone;

    @NotNull
    @Min(0)
    private Integer otpsAchieved;

    @NotNull
    @Min(0)
    private Integer interestedCustomers;

    @Size(max = 500)
    private String notes;
}
