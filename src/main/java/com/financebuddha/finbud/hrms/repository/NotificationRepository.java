package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Paginated list of a recipient's notifications, newest first. */
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    /** Paginated list of a recipient's *unread* notifications, newest first. */
    Page<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    /** Bell-badge count query — fires often, keep it cheap. */
    long countByRecipientIdAndIsReadFalse(Long recipientId);

    /**
     * Bulk-mark-as-read. Returns the number of rows flipped so the caller
     * can decide whether anything actually changed.
     */
    @Modifying
    @Query("""
           UPDATE Notification n
              SET n.isRead = true, n.readAt = :readAt
            WHERE n.recipient.id = :recipientId
              AND n.isRead = false
           """)
    int markAllReadForRecipient(Long recipientId, LocalDateTime readAt);
}
