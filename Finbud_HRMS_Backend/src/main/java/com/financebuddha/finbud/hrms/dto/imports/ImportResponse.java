package com.financebuddha.finbud.hrms.dto.imports;

import com.financebuddha.finbud.hrms.enums.ImportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for import operations with a detailed per-row summary.
 * <p>
 * Supports both the actual import path and the dry-run preview path
 * (toggled via the {@code dryRun} flag). In dry-run mode no rows are
 * persisted; only validation results are reported.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResponse {

    // NOTE: field initialisers (rather than @Builder.Default) so both
    // `new ImportResponse()` and `ImportResponse.builder().build()` start
    // with safe non-null counters — the increment helpers below assume that.
    @Builder.Default
    private Boolean dryRun = Boolean.FALSE;

    @Builder.Default
    private Boolean includeResigned = Boolean.FALSE;

    @Builder.Default
    private Integer totalRecords = 0;

    @Builder.Default
    private Integer successCount = 0;

    @Builder.Default
    private Integer failureCount = 0;

    @Builder.Default
    private Integer insertedCount = 0;

    @Builder.Default
    private Integer updatedCount = 0;

    @Builder.Default
    private Integer skippedCount = 0;

    @Builder.Default
    private List<ImportResult> results = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /** Sets any null counters to 0 — helpers below do in-place ++ and
     *  legacy callers may have left counters as null via @NoArgsConstructor. */
    private void ensureCountersInitialized() {
        if (totalRecords == null) totalRecords = 0;
        if (successCount == null) successCount = 0;
        if (failureCount == null) failureCount = 0;
        if (insertedCount == null) insertedCount = 0;
        if (updatedCount == null) updatedCount = 0;
        if (skippedCount == null) skippedCount = 0;
        if (results == null) results = new ArrayList<>();
        if (warnings == null) warnings = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportResult {
        private Integer rowNumber;
        private String employeeCode;
        private String employeeName;
        /**
         * Legacy raw string — kept for backward compatibility with the
         * pre-V4 ImportServiceImpl / ExcelImportServiceImpl call sites
         * that use {@code .status("SUCCESS"|"FAILED")}. New code should
         * set this through the typed helpers below (addInserted /
         * addUpdated / addSkipped / addFailure), which write canonical
         * {@link ImportStatus} values.
         */
        private String status;
        private String message;
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    public void addFailure(Integer rowNumber, String employeeCode, String message) {
        ensureCountersInitialized();
        this.results.add(ImportResult.builder()
                .rowNumber(rowNumber)
                .employeeCode(employeeCode)
                .status(ImportStatus.FAILED.name())
                .message(message)
                .build());
        this.failureCount = this.failureCount + 1;
    }

    public void addInserted(Integer rowNumber, String employeeCode, String employeeName, String message) {
        ensureCountersInitialized();
        this.results.add(ImportResult.builder()
                .rowNumber(rowNumber)
                .employeeCode(employeeCode)
                .employeeName(employeeName)
                .status(ImportStatus.IMPORTED.name())
                .message(message)
                .build());
        this.successCount = this.successCount + 1;
        this.insertedCount = this.insertedCount + 1;
    }

    public void addUpdated(Integer rowNumber, String employeeCode, String employeeName, String message) {
        ensureCountersInitialized();
        this.results.add(ImportResult.builder()
                .rowNumber(rowNumber)
                .employeeCode(employeeCode)
                .employeeName(employeeName)
                .status(ImportStatus.UPDATED.name())
                .message(message)
                .build());
        this.successCount = this.successCount + 1;
        this.updatedCount = this.updatedCount + 1;
    }

    public void addSkipped(Integer rowNumber, String employeeCode, String employeeName, String message) {
        ensureCountersInitialized();
        this.results.add(ImportResult.builder()
                .rowNumber(rowNumber)
                .employeeCode(employeeCode)
                .employeeName(employeeName)
                .status(ImportStatus.SKIPPED.name())
                .message(message)
                .build());
        this.skippedCount = this.skippedCount + 1;
    }

    /**
     * @deprecated Use {@link #addInserted} or {@link #addUpdated} instead so
     *             the summary counters stay accurate. Kept to avoid breaking
     *             legacy call sites in ImportServiceImpl / ExcelImportServiceImpl
     *             before Checkpoint 5 refactors them.
     */
    @Deprecated
    public void addSuccess(Integer rowNumber, String employeeCode, String employeeName, String message) {
        addInserted(rowNumber, employeeCode, employeeName, message);
    }
}
