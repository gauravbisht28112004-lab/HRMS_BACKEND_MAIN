package com.financebuddha.finbud.hrms.dto.notification;

import com.financebuddha.finbud.hrms.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Wire shape of a single in-app notification. Kept lean — the listing UI
 * renders the title, a relative timestamp, and deep-links using
 * {@code entityType} + {@code entityId}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private String title;
    private String body;
    private String entityType;
    private Long entityId;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
