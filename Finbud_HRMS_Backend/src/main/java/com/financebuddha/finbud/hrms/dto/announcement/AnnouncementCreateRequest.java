package com.financebuddha.finbud.hrms.dto.announcement;

import com.financebuddha.finbud.hrms.enums.AnnouncementPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Admin / HR payload for publishing a new announcement. */
@Data
public class AnnouncementCreateRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 4000)
    private String message;

    /** Defaults to MEDIUM at the service layer if the caller omits it. */
    @NotNull
    private AnnouncementPriority priority;
}
