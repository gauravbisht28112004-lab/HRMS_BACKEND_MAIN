package com.financebuddha.finbud.hrms.entity;

import com.financebuddha.finbud.hrms.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * In-app notification row. One per (recipient, event) pair.
 *
 * <p>Notifications are an append-only log — the only mutation is flipping
 * {@code isRead}/{@code readAt} when a recipient acknowledges. So we
 * intentionally don't extend {@link com.financebuddha.finbud.hrms.entity.base.BaseEntity}
 * (no {@code version}/{@code updatedAt} — those are wasted columns here).
 *
 * <p>{@code entityType} + {@code entityId} is a loose polymorphic pointer
 * back to the source record (e.g. {@code "LEAVE_REQUEST"} / 42). The UI
 * uses it to deep-link from the notification bell to the detail view.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_employee_id", nullable = false)
    private Employee recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "entity_type", length = 40)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
