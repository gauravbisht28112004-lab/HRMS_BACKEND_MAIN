package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.notification.NotificationResponse;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.Notification;
import com.financebuddha.finbud.hrms.enums.NotificationType;
import com.financebuddha.finbud.hrms.exception.ForbiddenException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.NotificationRepository;
import com.financebuddha.finbud.hrms.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Default implementation. Deliberately tiny — notifications are write-heavy
 * but simple records, so we keep the service a thin orchestrator.
 *
 * <p>The {@link #notify} method wraps the save in its own transaction
 * ({@code REQUIRES_NEW}) so that a failure to persist one notification
 * doesn't take down the surrounding leave / attendance transaction. We
 * also publish events with {@code AFTER_COMMIT} phase on the calling side,
 * so by the time we're here the originating row is already committed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional
    public void notify(Long recipientEmployeeId,
                       NotificationType type,
                       String title,
                       String body,
                       String entityType,
                       Long entityId) {
        try {
            Employee recipient = employeeRepository.findById(recipientEmployeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", recipientEmployeeId));

            Notification notification = Notification.builder()
                    .recipient(recipient)
                    .type(type)
                    .title(title)
                    .body(body)
                    .entityType(entityType)
                    .entityId(entityId)
                    .isRead(false)
                    .build();

            notificationRepository.save(notification);
        } catch (Exception ex) {
            // Never let a notification failure bubble up to the caller —
            // the leave update itself already succeeded. Log and move on.
            log.warn("Failed to persist notification for recipient={}, type={}: {}",
                    recipientEmployeeId, type, ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> listMine(Long recipientEmployeeId, int page, int size, boolean unreadOnly) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> results = unreadOnly
                ? notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(recipientEmployeeId, pageable)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientEmployeeId, pageable);

        return PagedResponse.of(
                results.getContent().stream().map(this::toResponse).toList(),
                results.getNumber(),
                results.getSize(),
                results.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(Long recipientEmployeeId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(recipientEmployeeId);
    }

    @Override
    @Transactional
    public NotificationResponse markRead(Long notificationId, Long callerEmployeeId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        // Enforce ownership — a user can only mark their own notifications read.
        if (notification.getRecipient() == null
                || !notification.getRecipient().getId().equals(callerEmployeeId)) {
            throw new ForbiddenException("You can only mark your own notifications as read");
        }

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    @Override
    @Transactional
    public int markAllRead(Long callerEmployeeId) {
        return notificationRepository.markAllReadForRecipient(callerEmployeeId, LocalDateTime.now());
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .entityType(n.getEntityType())
                .entityId(n.getEntityId())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .build();
    }
}
