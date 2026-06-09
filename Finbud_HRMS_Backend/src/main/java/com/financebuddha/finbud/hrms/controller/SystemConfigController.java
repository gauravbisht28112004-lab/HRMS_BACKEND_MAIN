package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.dto.sysconfig.OrgMonthlyGoalRequest;
import com.financebuddha.finbud.hrms.dto.sysconfig.OrgMonthlyGoalResponse;
import com.financebuddha.finbud.hrms.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Read/write surface for selected entries in the {@code system_config}
 * table that the dashboard / admin UI needs to surface or change.
 *
 * <p>Today this only exposes the org-wide monthly goal amount (Q5). When
 * we need more admin-tweakable knobs they go here too — keeping it a
 * dedicated controller (vs. extending AdminUserController) so the
 * authorization story stays clean.
 */
@RestController
@RequestMapping("/api/system-config")
@RequiredArgsConstructor
@Tag(name = "System Config", description = "Admin-tweakable application settings")
@SecurityRequirement(name = "bearerAuth")
public class SystemConfigController {

    private final SystemConfigService systemConfig;

    /**
     * Org-wide monthly disbursal goal. Readable by every authenticated
     * user — the dashboard tile shows the same value to everyone.
     */
    @GetMapping("/org-monthly-goal")
    @Operation(summary = "Read the org-wide monthly disbursal goal",
               description = "Any authenticated user. Returns 0 if Admin hasn't set it yet.")
    public ResponseEntity<ApiResponse<OrgMonthlyGoalResponse>> getOrgMonthlyGoal() {
        BigDecimal amount = systemConfig.getBigDecimal(
                SystemConfigService.Keys.ORG_MONTHLY_GOAL_AMOUNT, BigDecimal.ZERO);
        return ResponseEntity.ok(ApiResponse.success(
                OrgMonthlyGoalResponse.builder()
                        .amount(amount)
                        .currency("INR")
                        .build()));
    }

    /**
     * Set / update the org-wide monthly goal. Admin-only. The new value is
     * stored as a string in {@code system_config} and the in-memory cache
     * is invalidated so subsequent reads observe the change immediately.
     */
    @PutMapping("/org-monthly-goal")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update the org-wide monthly disbursal goal (Admin only)",
               description = "Stores the new amount in system_config and invalidates the read cache.")
    public ResponseEntity<ApiResponse<OrgMonthlyGoalResponse>> setOrgMonthlyGoal(
            @Valid @RequestBody OrgMonthlyGoalRequest request) {
        // Plain-string serialisation — BigDecimal#toPlainString avoids
        // scientific notation for huge numbers ("1E+7" type values).
        systemConfig.set(
                SystemConfigService.Keys.ORG_MONTHLY_GOAL_AMOUNT,
                request.getAmount().toPlainString(),
                "Org-wide monthly disbursal goal in INR, displayed on every dashboard.");
        return ResponseEntity.ok(ApiResponse.success(
                "Monthly goal updated",
                OrgMonthlyGoalResponse.builder()
                        .amount(request.getAmount())
                        .currency("INR")
                        .build()));
    }
}
