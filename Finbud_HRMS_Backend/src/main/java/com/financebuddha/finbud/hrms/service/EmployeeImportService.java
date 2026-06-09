package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.imports.EmployeeImportDTO;
import com.financebuddha.finbud.hrms.dto.imports.ImportResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Two-pass employee importer — the canonical write path for the
 * Finbud Noida master file. Consumed by both the JSON
 * {@code /api/employees/import} endpoint and the Excel upload endpoint
 * (via {@link ExcelImportService} delegation).
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Pass 1 — upsert each row as an Employee. Auto-create departments
 *         with a generated CODE when the named department doesn't exist.
 *         Capture the raw {@code reportingManagerCode} for Pass 2.</li>
 *     <li>Pass 2 — resolve manager references. Null-out a row that claims
 *         itself as its own manager.</li>
 *     <li>User provisioning — for each imported Employee, ensure a {@code User}
 *         exists with {@code ROLE_EMPLOYEE} (from {@code auth.default_role}) and
 *         the default password from {@code auth.default_password}. Never grants
 *         ADMIN/HR automatically; those are assigned manually by an admin.</li>
 *     <li>Dry-run mode — when {@link Options#isDryRun()} is true, no writes
 *         occur and the response only reports validation + would-be results.</li>
 *     <li>Resigned-row handling — by default, rows with a RESIGNED status are
 *         skipped. Set {@link Options#isIncludeResigned()} to true to process
 *         them too (for historical backfill).</li>
 * </ul>
 */
public interface EmployeeImportService {

    ImportResponse importEmployees(List<EmployeeImportDTO> rows, Options options);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class Options {
        /** If true, validate + preview without writing to the database. */
        @Builder.Default
        private boolean dryRun = false;

        /** If true, process rows whose status looks like RESIGNED / INACTIVE. */
        @Builder.Default
        private boolean includeResigned = false;

        /** If true, create a User account for each imported Employee. Default true. */
        @Builder.Default
        private boolean createUsers = true;
    }
}
