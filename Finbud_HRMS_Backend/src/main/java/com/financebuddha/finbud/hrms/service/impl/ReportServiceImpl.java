package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.LeaveRequest;
import com.financebuddha.finbud.hrms.entity.Payroll;
import com.financebuddha.finbud.hrms.repository.LeaveRequestRepository;
import com.financebuddha.finbud.hrms.repository.PayrollRepository;
import com.financebuddha.finbud.hrms.service.ReportService;
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
import java.util.function.BiConsumer;

/**
 * Apache POI-based Excel builder for the admin Reports dashboard (Leave,
 * Payroll). Mirrors {@link CommitmentReportServiceImpl}: single sheet, bold
 * header row, one entity per row, auto-sized columns.
 *
 * <p>Each public method runs in a read-only transaction so the lazy
 * {@code employee} / {@code department} associations can be walked while the
 * Hibernate session is still open.
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollRepository payrollRepository;

    // ====================== LEAVE ======================

    private static final String[] LEAVE_COLUMNS = {
            "Employee Code", "Employee Name", "Department", "Leave Type",
            "Start Date", "End Date", "Days", "Half Day", "Status",
            "Approved By", "Reason"
    };

    @Override
    @Transactional(readOnly = true)
    public byte[] leaveXlsx(LocalDate startDate, LocalDate endDate, Long departmentId) throws IOException {
        List<LeaveRequest> rows = leaveRequestRepository.findForReport(startDate, endDate, departmentId);
        return build("Leave", LEAVE_COLUMNS, rows, (row, lr) -> {
            Employee emp = lr.getEmployee();
            Employee approver = lr.getApprovedBy();
            row.createCell(0).setCellValue(emp != null ? str(emp.getEmployeeId()) : "");
            row.createCell(1).setCellValue(emp != null ? str(emp.getFullName()) : "");
            row.createCell(2).setCellValue(departmentName(emp));
            row.createCell(3).setCellValue(lr.getLeaveType() != null ? lr.getLeaveType().name() : "");
            row.createCell(4).setCellValue(str(lr.getStartDate()));
            row.createCell(5).setCellValue(str(lr.getEndDate()));
            row.createCell(6).setCellValue(dbl(lr.getDaysRequested()));
            row.createCell(7).setCellValue(yesNo(lr.getIsHalfDay()));
            row.createCell(8).setCellValue(lr.getStatus() != null ? lr.getStatus().name() : "");
            row.createCell(9).setCellValue(approver != null ? str(approver.getFullName()) : "");
            row.createCell(10).setCellValue(str(lr.getReason()));
        });
    }

    // ====================== PAYROLL ======================

    private static final String[] PAYROLL_COLUMNS = {
            "Month", "Year", "Employee Code", "Employee Name", "Department",
            "Working Days", "Present Days", "Absent Days", "Leave Days",
            "Gross Earnings", "Total Deductions", "Net Pay",
            "Overtime Hours", "Overtime Pay", "Status"
    };

    @Override
    @Transactional(readOnly = true)
    public byte[] payrollXlsx(LocalDate startDate, LocalDate endDate, Long departmentId) throws IOException {
        // Payroll is stored per (year, month). Flatten the requested window
        // into a single ordinal so the repository can range-filter on it.
        int startKey = startDate.getYear() * 12 + startDate.getMonthValue();
        int endKey = endDate.getYear() * 12 + endDate.getMonthValue();
        List<Payroll> rows = payrollRepository.findForReport(startKey, endKey, departmentId);
        return build("Payroll", PAYROLL_COLUMNS, rows, (row, p) -> {
            Employee emp = p.getEmployee();
            row.createCell(0).setCellValue(num(p.getMonth()));
            row.createCell(1).setCellValue(num(p.getYear()));
            row.createCell(2).setCellValue(emp != null ? str(emp.getEmployeeId()) : "");
            row.createCell(3).setCellValue(emp != null ? str(emp.getFullName()) : "");
            row.createCell(4).setCellValue(departmentName(emp));
            row.createCell(5).setCellValue(num(p.getTotalWorkingDays()));
            row.createCell(6).setCellValue(dbl(p.getPresentDays()));
            row.createCell(7).setCellValue(dbl(p.getAbsentDays()));
            row.createCell(8).setCellValue(dbl(p.getLeaveDays()));
            row.createCell(9).setCellValue(dbl(p.getGrossEarnings()));
            row.createCell(10).setCellValue(dbl(p.getTotalDeductions()));
            row.createCell(11).setCellValue(dbl(p.getNetPay()));
            row.createCell(12).setCellValue(dbl(p.getOvertimeHours()));
            row.createCell(13).setCellValue(dbl(p.getOvertimePay()));
            row.createCell(14).setCellValue(p.getStatus() != null ? p.getStatus().name() : "");
        });
    }

    // ====================== SHARED WORKBOOK BUILDER ======================

    /**
     * Generic single-sheet workbook builder. Memory is bounded by streaming
     * rows directly — fine up to ~10k rows; beyond that we'd switch to
     * SXSSFWorkbook. Today's report volumes are well under that.
     */
    private <T> byte[] build(String sheetName, String[] columns, List<T> rows,
                             BiConsumer<Row, T> filler) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            Row header = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (T item : rows) {
                Row row = sheet.createRow(rowIdx++);
                filler.accept(row, item);
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ====================== CELL VALUE HELPERS ======================

    private static String departmentName(Employee emp) {
        return emp != null && emp.getDepartment() != null && emp.getDepartment().getName() != null
                ? emp.getDepartment().getName() : "";
    }

    private static String str(Object value) {
        return value != null ? value.toString() : "";
    }

    private static String str(LocalDate value) {
        return value != null ? value.toString() : "";
    }

    private static int num(Integer value) {
        return value != null ? value : 0;
    }

    private static double dbl(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    private static String yesNo(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Yes" : "No";
    }
}
