package com.financebuddha.finbud.hrms.dto.announcement;

import com.financebuddha.finbud.hrms.enums.AnnouncementPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Wire shape of an announcement for the dashboard listing. Includes the
 * creator's name (resolved at mapping time) so the UI doesn't have to do
 * a second lookup.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponse {
    private Long id;
    private String title;
    private String message;
    private AnnouncementPriority priority;
    private Boolean isActive;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
