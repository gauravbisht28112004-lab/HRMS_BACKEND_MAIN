package com.financebuddha.finbud.hrms.entity;

import com.financebuddha.finbud.hrms.entity.base.BaseEntity;
import com.financebuddha.finbud.hrms.enums.AnnouncementPriority;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Admin / HR-published announcement, visible on every authenticated user's
 * dashboard. Soft-deleted via {@link #isActive} so accidentally-archived
 * notices can be restored — and the audit trail (who created it, when)
 * stays intact.
 */
@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Announcement extends BaseEntity {

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    @Builder.Default
    private AnnouncementPriority priority = AnnouncementPriority.MEDIUM;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * The employee who published the announcement. Nullable=false at the
     * column level; we always have an authenticated user creating these.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_employee_id", nullable = false)
    private Employee createdByEmployee;
}
