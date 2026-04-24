package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.imports.EmployeeImportDTO;
import com.financebuddha.finbud.hrms.dto.imports.ImportResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for Excel import operations.
 * <p>
 * The legacy single-arg {@link #importEmployees(MultipartFile)} call uses
 * the default {@link EmployeeImportService.Options} — i.e. real writes,
 * skip resigned, create users. New callers should prefer the overload
 * that takes an {@link EmployeeImportService.Options} so they can opt
 * into {@code dryRun}, {@code includeResigned}, etc.
 */
public interface ExcelImportService {

    /**
     * Import employees from an Excel file using default options.
     * @param file The multipart file to import
     * @return ImportResponse with detailed results
     */
    ImportResponse importEmployees(MultipartFile file) throws Exception;

    /**
     * Parse the Excel file and run the canonical two-pass import with the
     * caller-supplied options. Honours {@code dryRun} (no DB writes),
     * {@code includeResigned} (process inactive rows), and
     * {@code createUsers} (skip user provisioning).
     */
    ImportResponse importEmployees(MultipartFile file, EmployeeImportService.Options options) throws Exception;

    /**
     * Parse Excel file and return list of DTOs (for preview)
     * @param file The multipart file to parse
     * @return List of EmployeeImportDTO
     */
    List<EmployeeImportDTO> parseExcel(MultipartFile file) throws Exception;

    /**
     * Clean up all employee data (except system users if specified)
     * @param preserveSystemUsers Whether to preserve admin/HR system users
     */
    void cleanupEmployeeData(boolean preserveSystemUsers);
}
