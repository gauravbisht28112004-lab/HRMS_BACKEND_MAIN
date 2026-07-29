package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.announcement.AnnouncementCreateRequest;
import com.financebuddha.finbud.hrms.dto.announcement.AnnouncementResponse;
import com.financebuddha.finbud.hrms.dto.announcement.AnnouncementUpdateRequest;
import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;

import java.util.List;

public interface AnnouncementService {

    /** Active announcements newest-first — every authenticated user sees this. */
    List<AnnouncementResponse> listActive();

    /** Paginated full list incl. archived — Admin/HR only. */
    PagedResponse<AnnouncementResponse> listAll(PaginationRequest paginationRequest);

    AnnouncementResponse getById(Long id);

    /** Publishes a new announcement on behalf of {@code creatorEmployeeId}. Admin/HR only. */
    AnnouncementResponse create(Long creatorEmployeeId, AnnouncementCreateRequest request);

    /** Patches one or more fields. Admin/HR only. */
    AnnouncementResponse update(Long id, AnnouncementUpdateRequest request);

    /** Soft-delete via {@code is_active=false}. Idempotent. Admin/HR only. */
    void deactivate(Long id);
}
