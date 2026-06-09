package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.notification.NotificationResponse;
import com.financebuddha.finbud.hrms.entity.User;
import com.financebuddha.finbud.hrms.exception.ForbiddenException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.repository.UserRepository;
import com.financebuddha.finbud.hrms.security.CurrentUser;
import com.financebuddha.finbud.hrms.security.UserPrincipal;
import com.financebuddha.finbud.hrms.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * In-app notification endpoints. Everything is scoped to the calling user's
 * own notifications — no cross-user reads. The bell badge in the topbar
 * polls {@link #unreadCount} on a timer, and the dropdown calls
 * {@link #list} with {@code unreadOnly=true, size=10}.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification feed")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "List my notifications",
            description = "Paginated list of the current user's notifications, newest first. Filter with ?unreadOnly=true.")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> list(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        Long employeeId = resolveEmployeeId(currentUser);
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.listMine(employeeId, page, size, unreadOnly)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Unread count", description = "Just the number — for the bell badge.")
    public ResponseEntity<ApiResponse<Long>> unreadCount(@CurrentUser UserPrincipal currentUser) {
        Long employeeId = resolveEmployeeId(currentUser);
        return ResponseEntity.ok(ApiResponse.success(notificationService.unreadCount(employeeId)));
    }

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "Mark one as read", description = "Flip a single notification's isRead flag. 403 if it isn't yours.")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long notificationId) {
        Long employeeId = resolveEmployeeId(currentUser);
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.markRead(notificationId, employeeId)));
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all as read", description = "Bulk-flip every unread notification belonging to the caller.")
    public ResponseEntity<ApiResponse<Integer>> markAllRead(@CurrentUser UserPrincipal currentUser) {
        Long employeeId = resolveEmployeeId(currentUser);
        return ResponseEntity.ok(ApiResponse.success(notificationService.markAllRead(employeeId)));
    }

    /**
     * Resolve the caller's {@code Employee.id}. {@link UserPrincipal#getId()}
     * is the {@code User.id} — we traverse through the user to the linked
     * employee. Callers without a linked employee cannot have notifications,
     * so we 403 rather than silently return an empty list.
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
