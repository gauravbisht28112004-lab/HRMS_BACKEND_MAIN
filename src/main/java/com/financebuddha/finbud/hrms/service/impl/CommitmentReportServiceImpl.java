package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.entity.DailyCommitment;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.repository.DailyCommitmentRepository;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.service.CommitmentReportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Apache POI-based Excel report builder for daily commitments.
 * Q1 Phase D Part 2.
 *
 * <p>Two reports today: per-employee history and per-team history. Both
 * share the same column layout for consistency in HR's downstream tooling.
 */
@Service
@RequiredArgsConstructor
public class CommitmentReportServiceImpl implements CommitmentReportService {

    private final DailyCommitmentRepository dailyCommitmentRepository;
    private final EmployeeRepository employeeRepository;

    private static final String[] COLUMNS = {
            "Date", "Employee Code", "Employee Name",
            "Target Calls", "Actual Calls",
            "Target OTPs", "Actual OTPs",
            "Target Customers", "Actual Customers",
            "Target Disbursal (₹)", "Actual Disbursal (₹)",
            "Status", "Submitted At", "Reviewed By", "Notes"
    };

    @Override
    @Transactional(readOnly = true)
    public byte[] employeeDailyXlsx(Long employeeId, LocalDate startDate, LocalDate endDate) throws IOException {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        List<DailyCommitment> rows = dailyCommitmentRepository
                .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDesc(employeeId, startDate, endDate);

        String sheetName = ("Commitments " + employee.getEmployeeId()).substring(
                0, Math.min(31, ("Commitments " + employee.getEmployeeId()).length()));
        return buildWorkbook(sheetName, rows);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] teamDailyXlsx(Long managerId, LocalDate startDate, LocalDate endDate) throws IOException {
        // Pull all team members' commitments. Reuse the manager-pending
        // query path's join with a date-range filter expressed in JPQL.
        List<DailyCommitment> rows = dailyCommitmentRepository
                .findByManagerIdAndWorkDateBetween(managerId, startDate, endDate);

        return buildWorkbook("Team Commitments", rows);
    }

    /**
     * Common workbook builder. Tries to keep memory bounded by streaming
     * rows directly — this is fine up to ~10k rows; for larger reports we
     * would switch to SXSSFWorkbook. Today's volumes are well under that.
     */
    private byte[] buildWorkbook(String sheetName, List<DailyCommitment> rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);

            // Header style — bold + a thin bottom border so HR can paste the
            // file straight into another spreadsheet without losing context.
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            Row header = sheet.createRow(0);
            for (int i = 0; i < COLUMNS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(COLUMNS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (DailyCommitment c : rows) {
                Row row = sheet.createRow(rowIdx++);
                Employee emp = c.getEmployee();
                Employee reviewer = c.getApprovedBy();
                row.createCell(0).setCellValue(c.getWorkDate() != null ? c.getWorkDate().toString() : "");
                row.createCell(1).setCellValue(emp != null && emp.getEmployeeId() != null ? emp.getEmployeeId() : "");
                row.createCell(2).setCellValue(emp != null && emp.getFullName() != null ? emp.getFullName() : "");
                row.createCell(3).setCellValue(orZero(c.getTargetCalls()));
                row.createCell(4).setCellValue(orZero(c.getActualCalls()));
                row.createCell(5).setCellValue(orZero(c.getTargetOtps()));
                row.createCell(6).setCellValue(orZero(c.getActualOtps()));
                row.createCell(7).setCellValue(orZero(c.getTargetInterestedCustomers()));
                row.createCell(8).setCellValue(orZero(c.getActualInterestedCustomers()));
                row.createCell(9).setCellValue(asDouble(c.getTargetDisbursalAmount()));
                row.createCell(10).setCellValue(asDouble(c.getActualDisbursalAmount()));
                row.createCell(11).setCellValue(c.getStatus() != null ? c.getStatus().name() : "");
                row.createCell(12).setCellValue(c.getSubmittedAt() != null ? c.getSubmittedAt().toString() : "");
                row.createCell(13).setCellValue(reviewer != null && reviewer.getFullName() != null
                        ? reviewer.getFullName() : "");
                row.createCell(14).setCellValue(c.getNotes() != null ? c.getNotes() : "");
            }

            for (int i = 0; i < COLUMNS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static int orZero(Integer value) {
        return value != null ? value : 0;
    }

    private static double asDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }
}
