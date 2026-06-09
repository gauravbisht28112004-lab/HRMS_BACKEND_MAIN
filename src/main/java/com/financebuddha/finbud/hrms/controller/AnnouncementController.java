package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.announcement.AnnouncementCreateRequest;
import com.financebuddha.finbud.hrms.dto.announcement.AnnouncementResponse;
import com.financebuddha.finbud.hrms.dto.announcement.AnnouncementUpdateRequest;
import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.exception.ForbiddenException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.repository.UserRepository;
import com.financebuddha.finbud.hrms.security.CurrentUser;
import com.financebuddha.finbud.hrms.security.UserPrincipal;
import com.financebuddha.finbud.hrms.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Announcement management. Q2 of the Apr-25 feature triage.
 *
 * <p>Authorization split:
 * <ul>
 *   <li>{@code GET /api/announcements} — any authenticated user, returns
 *       active announcements only. This is what every dashboard reads.</li>
 *   <li>Everything else (list-all incl. archived, create, update,
 *       deactivate) requires {@code ROLE_ADMIN} or {@code ROLE_HR}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
@Tag(name = "Announcements", description = "Admin / HR-published dashboard notices")
@SecurityRequirement(name = "bearerAuth")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "List active announcements",
               description = "Any authenticated user. Returns active announcements newest-first; the dashboard reads this.")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> listActive() {
        return ResponseEntity.ok(ApiResponse.success(announcementService.listActive()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "List all announcements (including archived)",
               description = "Admin/HR only. Used by the management screen so HR can restore a soft-deleted notice.")
    public ResponseEntity<ApiResponse<PagedResponse<AnnouncementResponse>>> listAll(
            @ParameterObject PaginationRequest paginationRequest) {
        return ResponseEntity.ok(ApiResponse.success(announcementService.listAll(paginationRequest)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single announcement", description = "Any authenticated user.")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(announcementService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Publish a new announcement", description = "Admin/HR only.")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> create(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody AnnouncementCreateRequest request) {
        AnnouncementResponse response = announcementService.create(resolveEmployeeId(currentUser), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Announcement published", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Update an announcement", description = "Patch any subset of fields. Admin/HR only.")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody AnnouncementUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Announcement updated",
                announcementService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Archive an announcement (soft delete)",
               description = "Sets is_active=false. Admin/HR only. Idempotent.")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        announcementService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Announcement archived", null));
    }

    /**
     * {@link UserPrincipal#getId()} returns the User row id; the service
     * needs the linked Employee row id (so we can record who created the
     * announcement). One small lookup per write — cheap and avoids the
     * pre-existing User.id-as-Employee.id confusion in older controllers.
     */
    private Long resolveEmployeeId(UserPrincipal principal) {
        if (principal == null) {
            throw new ForbiddenException("Unauthenticated");
        }
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));
        if (user.getEmployee() == null) {
            throw new ForbiddenException("Your login is not linked to an employee — ask HR to provision your profile.");
        }
        return user.getEmployee().getId();
    }
}
