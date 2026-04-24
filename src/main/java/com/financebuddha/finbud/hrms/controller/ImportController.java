package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.dto.imports.EmployeeImportDTO;
import com.financebuddha.finbud.hrms.dto.imports.ImportResponse;
import com.financebuddha.finbud.hrms.service.EmployeeImportService;
import com.financebuddha.finbud.hrms.service.ExcelImportService;
import com.financebuddha.finbud.hrms.service.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Bulk-import endpoints for HR / admin operators.
 * <p>
 * The two flagship endpoints are:
 * <ul>
 *   <li>{@code POST /api/admin/import/employees} — full import. Accepts
 *       {@code dryRun}, {@code includeResigned}, {@code createUsers} query
 *       params so HR can preview before writing.</li>
 *   <li>{@code POST /api/admin/import/employees/preview} — header parse only;
 *       returns parsed DTOs for UI inspection (no DB hit).</li>
 * </ul>
 * <p>
 * Role provisioning is intentionally NOT done here — the import always
 * grants ROLE_EMPLOYEE; ADMIN/HR/MANAGER must be granted via
 * {@code POST /api/admin/users}. See C-1 in the hardening plan.
 */
@RestController
@RequestMapping("/api/admin/import")
@RequiredArgsConstructor
@Tag(name = "Data Import", description = "Bulk data import APIs for administrators")
@SecurityRequirement(name = "bearerAuth")
public class ImportController {

    private final ExcelImportService excelImportService;
    private final ImportService importService;

    /**
     * Import employees from Excel using the canonical two-pass importer.
     * <p>
     * Query params:
     * <ul>
     *   <li>{@code dryRun} (default {@code false}) — validate and report
     *       per-row outcomes without writing to the DB.</li>
     *   <li>{@code includeResigned} (default {@code false}) — process rows
     *       whose status looks like RESIGNED / INACTIVE (historical backfill).</li>
     *   <li>{@code createUsers} (default {@code true}) — create a User
     *       account with ROLE_EMPLOYEE for each imported Employee.</li>
     * </ul>
     */
    @PostMapping("/employees")
    @Operation(summary = "Import employees from Excel",
               description = "Two-pass upsert with optional dry-run preview. "
                           + "Always grants ROLE_EMPLOYEE; admin/HR/manager grants are a separate manual step.")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<ImportResponse>> importEmployees(
            @Parameter(description = "Excel file (.xlsx or .xls) with employee data")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Validate without persisting any rows")
            @RequestParam(value = "dryRun", defaultValue = "false") boolean dryRun,
            @Parameter(description = "Process rows whose status is RESIGNED/INACTIVE")
            @RequestParam(value = "includeResigned", defaultValue = "false") boolean includeResigned,
            @Parameter(description = "Provision a User account per Employee row")
            @RequestParam(value = "createUsers", defaultValue = "true") boolean createUsers) throws Exception {

        EmployeeImportService.Options options = EmployeeImportService.Options.builder()
                .dryRun(dryRun)
                .includeResigned(includeResigned)
                .createUsers(createUsers)
                .build();

        ImportResponse response = excelImportService.importEmployees(file, options);
        String summary = (dryRun ? "Dry-run complete: " : "Import complete: ")
                + response.getInsertedCount() + " inserted, "
                + response.getUpdatedCount() + " updated, "
                + response.getSkippedCount() + " skipped, "
                + response.getFailureCount() + " failed";
        return ResponseEntity.ok(ApiResponse.success(summary, response));
    }

    /**
     * Preview Excel file - parses and returns data without importing.
     * Useful for validating data shape before actual import.
     */
    @PostMapping("/employees/preview")
    @Operation(summary = "Preview Excel data",
               description = "Parse and preview Excel data without importing. Returns row-level DTOs for UI inspection.")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<List<EmployeeImportDTO>>> previewEmployees(
            @RequestParam("file") MultipartFile file) throws Exception {
        List<EmployeeImportDTO> employees = excelImportService.parseExcel(file);
        return ResponseEntity.ok(ApiResponse.success(
                "Found " + employees.size() + " valid employee records",
                employees));
    }

    /**
     * Clean up imported employee data. ADMIN-only.
     * Reserved Finbud codes (ND33004, ND33301) are preserved unconditionally.
     */
    @DeleteMapping("/employees/cleanup")
    @Operation(summary = "Clean up employee data",
               description = "Delete imported employee data. Reserved Finbud codes are preserved.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> cleanupEmployees(
            @RequestParam(value = "preserveSystemUsers", defaultValue = "true") boolean preserveSystemUsers) {
        excelImportService.cleanupEmployeeData(preserveSystemUsers);
        return ResponseEntity.ok(ApiResponse.success(
                "Employee data cleanup completed. System users preserved: " + preserveSystemUsers,
                null));
    }

    /**
     * Legacy single-arg path. Kept as a thin shim so older HRMS integrations
     * that POST without query params still work — they get default options
     * (real writes, skip resigned, create users).
     */
    @PostMapping("/employees/legacy")
    @Operation(summary = "Legacy import endpoint",
               description = "Use /api/admin/import/employees with query params instead.")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<ApiResponse<ImportResponse>> legacyImportEmployees(
            @RequestParam("file") MultipartFile file) throws Exception {
        ImportResponse response = importService.importEmployees(file);
        return ResponseEntity.ok(ApiResponse.success(
                "Legacy import: " + response.getSuccessCount() + " success, " + response.getFailureCount() + " failed",
                response));
    }
}
