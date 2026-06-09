package com.financebuddha.finbud.hrms.repository;

import com.financebuddha.finbud.hrms.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /** Active announcements newest-first — what every dashboard shows. */
    List<Announcement> findByIsActiveTrueOrderByCreatedAtDesc();

    /** Paginated full list (incl. archived) for the Admin/HR management screen. */
    Page<Announcement> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
