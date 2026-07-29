package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.notification.NotificationResponse;
import com.financebuddha.finbud.hrms.enums.NotificationType;

public interface NotificationService {

    /**
     * Persist a single in-app notification for one recipient. Used by the
     * leave/attendance/payroll listeners to fan out status changes.
     *
     * @param recipientEmployeeId employee.id of the notification target
     * @param type                one of {@link NotificationType}
     * @param title               short headline (≤ 200 chars)
     * @param body                optional longer text — may be null
     * @param entityType          e.g. "LEAVE_REQUEST" for deep-linking
     * @param entityId            id of the source record
     */
    void notify(Long recipientEmployeeId,
                NotificationType type,
                String title,
                String body,
                String entityType,
                Long entityId);

    /** Paginated list of the calling user's notifications, newest first. */
    PagedResponse<NotificationResponse> listMine(Long recipientEmployeeId, int page, int size, boolean unreadOnly);

    /** Bell-badge count — unread notifications for the calling user. */
    long unreadCount(Long recipientEmployeeId);

    /** Mark a single notification as read; no-op if already read. 403 if not owned by caller. */
    NotificationResponse markRead(Long notificationId, Long callerEmployeeId);

    /** Mark all of the caller's unread notifications as read. Returns the number flipped. */
    int markAllRead(Long callerEmployeeId);
}
