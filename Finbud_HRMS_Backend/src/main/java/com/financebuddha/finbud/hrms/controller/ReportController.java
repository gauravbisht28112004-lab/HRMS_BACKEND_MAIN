package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.exception.BadRequestException;
import com.financebuddha.finbud.hrms.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Excel report endpoints for the admin Reports dashboard. Each endpoint
 * streams an .xlsx as raw bytes; the frontend handles the download via
 * arraybuffer → Blob → ObjectURL, the same way the commitment reports and
 * payslip download work.
 *
 * <p>All endpoints are restricted to ADMIN / HR / MANAGER (also enforced at
 * the URL level in SecurityConfig for {@code /api/reports/**}). A
 * {@code departmentId} of {@code null} means "all departments".
 *
 * <p>Note: paths here ({@code /leave.xlsx} etc.) do not collide with
 * {@code /api/reports/commitment/**}, which is served by its own controller.
 */
@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Leave and payroll Excel exports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReportService reportService;

    @GetMapping("/leave.xlsx")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Leave report (Excel) for a date range, optional department")
    public ResponseEntity<byte[]> leaveReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long departmentId) throws IOException {
        validateDateWindow(startDate, endDate);
        byte[] bytes = reportService.leaveXlsx(startDate, endDate, departmentId);
        return excelResponse("leave-%s-to-%s.xlsx".formatted(startDate, endDate), bytes);
    }

    @GetMapping("/payroll.xlsx")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Payroll report (Excel) for the months in a date range, optional department")
    public ResponseEntity<byte[]> payrollReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long departmentId) throws IOException {
        validateDateWindow(startDate, endDate);
        byte[] bytes = reportService.payrollXlsx(startDate, endDate, departmentId);
        return excelResponse("payroll-%s-to-%s.xlsx".formatted(startDate, endDate), bytes);
    }

    /**
     * Reject report windows that reach before today. Trial data was wiped
     * before go-live, so the dashboard is scoped to today onward only. The
     * frontend already enforces this (min date on the pickers); this guards
     * the API against hand-crafted requests. Also rejects an inverted window.
     */
    private void validateDateWindow(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        if (startDate.isBefore(today)) {
            throw new BadRequestException(
                    "Reports are available from today (" + today + ") onward only.");
        }
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("End date must be on or after the start date.");
        }
    }

    /** Common excel-stream response with the right MIME + Content-Disposition. */
    private ResponseEntity<byte[]> excelResponse(String filename, byte[] bytes) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(XLSX_MIME));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(bytes.length);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}
