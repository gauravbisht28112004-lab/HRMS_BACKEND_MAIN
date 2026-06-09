package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.announcement.AnnouncementCreateRequest;
import com.financebuddha.finbud.hrms.dto.announcement.AnnouncementResponse;
import com.financebuddha.finbud.hrms.dto.announcement.AnnouncementUpdateRequest;
import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.entity.Announcement;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.enums.AnnouncementPriority;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.repository.AnnouncementRepository;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listActive() {
        return announcementRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AnnouncementResponse> listAll(PaginationRequest paginationRequest) {
        Pageable pageable = PageRequest.of(
                paginationRequest.getPage(),
                paginationRequest.getSize());
        Page<Announcement> page = announcementRepository.findAllByOrderByCreatedAtDesc(pageable);
        return PagedResponse.of(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public AnnouncementResponse getById(Long id) {
        return toResponse(loadOrThrow(id));
    }

    @Override
    @Transactional
    public AnnouncementResponse create(Long creatorEmployeeId, AnnouncementCreateRequest request) {
        Employee creator = employeeRepository.findById(creatorEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", creatorEmployeeId));

        Announcement announcement = Announcement.builder()
                .title(request.getTitle().trim())
                .message(request.getMessage().trim())
                .priority(request.getPriority() != null ? request.getPriority() : AnnouncementPriority.MEDIUM)
                .isActive(true)
                .createdByEmployee(creator)
                .build();

        Announcement saved = announcementRepository.save(announcement);
        log.info("Announcement #{} published by employee {}", saved.getId(), creator.getEmployeeId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AnnouncementResponse update(Long id, AnnouncementUpdateRequest request) {
        Announcement announcement = loadOrThrow(id);

        // Patch only the fields the caller actually sent. Trim strings so
        // a stray newline at the end of a paste doesn't sit in the DB.
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            announcement.setTitle(request.getTitle().trim());
        }
        if (request.getMessage() != null && !request.getMessage().isBlank()) {
            announcement.setMessage(request.getMessage().trim());
        }
        if (request.getPriority() != null) {
            announcement.setPriority(request.getPriority());
        }
        if (request.getIsActive() != null) {
            announcement.setIsActive(request.getIsActive());
        }

        return toResponse(announcementRepository.save(announcement));
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Announcement announcement = loadOrThrow(id);
        if (Boolean.FALSE.equals(announcement.getIsActive())) {
            return; // already archived — idempotent no-op
        }
        announcement.setIsActive(false);
        announcementRepository.save(announcement);
        log.info("Announcement #{} archived", id);
    }

    private Announcement loadOrThrow(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));
    }

    private AnnouncementResponse toResponse(Announcement a) {
        Employee creator = a.getCreatedByEmployee();
        return AnnouncementResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .message(a.getMessage())
                .priority(a.getPriority())
                .isActive(a.getIsActive())
                .createdById(creator != null ? creator.getId() : null)
                .createdByName(creator != null ? creator.getFullName() : null)
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
