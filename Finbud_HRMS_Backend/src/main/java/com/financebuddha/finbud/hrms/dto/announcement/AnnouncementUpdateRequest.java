package com.financebuddha.finbud.hrms.dto.announcement;

import com.financebuddha.finbud.hrms.enums.AnnouncementPriority;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Update payload — all fields optional. Pass {@code isActive=false} to
 * archive an announcement (soft delete). The service ignores null
 * fields so callers can patch a single attribute (e.g. just toggle
 * priority) without resending the whole record.
 */
@Data
public class AnnouncementUpdateRequest {

    @Size(max = 200)
    private String title;

    @Size(max = 4000)
    private String message;

    private AnnouncementPriority priority;

    private Boolean isActive;
}
