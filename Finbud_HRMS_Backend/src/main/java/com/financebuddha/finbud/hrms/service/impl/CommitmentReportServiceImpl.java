package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.entity.DailyCommitment;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.enums.CommitmentStatus;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Apache POI-based Excel report builder for daily commitments.
 *
 * <p>Every workbook has two sheets:
 * <ul>
 *   <li><b>Daily Detail</b> — one row per employee per workday, every status,
 *       exactly as captured. This is the audit trail.</li>
 *   <li><b>Employee Summary</b> — one row per employee with range totals and
 *       achievement %. Includes roster members who filed nothing in the range
 *       (as zeros), because "this person reported nothing for three weeks" is
 *       the single most useful thing in a performance report.</li>
 * </ul>
 *
 * <p><b>Status basis.</b> The detail sheet carries every row regardless of
 * status. The summary's disbursal total counts <em>APPROVED rows only</em>, so
 * it reconciles exactly with the "Team Disbursed" figure on the hierarchy
 * dashboard (which uses
 * {@code DailyCommitmentRepository#aggregateSubtreeDisbursalByBranch}, also
 * APPROVED-only). Activity counts — calls, OTPs, customers — are summed across
 * all rows; the "Days Reported" vs "Days Approved" columns expose the split.
 *
 * <p><b>Scope.</b> The team report covers the supervisor's whole management
 * subtree (Manager → Team Leader → ATL → Employee), not just direct reports.
 * Because the hierarchy is a single-parent tree, a subtree walk from one
 * manager can never reach another manager's branch — that is what guarantees
 * a Manager only ever sees TLs and ATLs beneath themselves.
 */
@Service
@RequiredArgsConstructor
public class CommitmentReportServiceImpl implements CommitmentReportService {

    private final DailyCommitmentRepository dailyCommitmentRepository;
    private final EmployeeRepository employeeRepository;

    private static final String[] DETAIL_COLUMNS = {
            "Date", "Employee Code", "Employee Name",
            "Target Calls", "Actual Calls",
            "Target OTPs", "Actual OTPs",
            "Target Customers", "Actual Customers",
            "Target Disbursal (₹)", "Actual Disbursal (₹)",
            "Status", "Submitted At", "Reviewed By", "Notes",
            // Appended last on purpose — the team sheet now spans several
            // levels of the hierarchy, so the row's own supervisor is needed
            // to tell branches apart. Kept at the end so existing column
            // positions in HR's downstream tooling don't shift.
            "Reports To"
    };

    private static final String[] SUMMARY_COLUMNS = {
            "Employee Code", "Employee Name", "Reports To",
            "Days Reported", "Days Approved",
            "Target Calls", "Actual Calls",
            "Target OTPs", "Actual OTPs",
            "Target Customers", "Actual Customers",
            "Target Disbursal (₹)", "Approved Disbursal (₹)", "Achievement %"
    };

    private static final String DETAIL_SHEET = "Daily Detail";
    private static final String SUMMARY_SHEET = "Employee Summary";

    @Override
    @Transactional(readOnly = true)
    public byte[] employeeDailyXlsx(Long employeeId, LocalDate startDate, LocalDate endDate) throws IOException {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));

        List<DailyCommitment> rows = dailyCommitmentRepository
                .findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDesc(employeeId, startDate, endDate);

        return buildWorkbook(List.of(employee), rows);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] teamDailyXlsx(Long managerId, LocalDate startDate, LocalDate endDate) throws IOException {
        // Whole management subtree, not just direct reports. The chain is
        // Manager → Team Leader → ATL → Employee and only the leaf tier files
        // daily commitments, so the old one-level manager_id lookup produced an
        // empty sheet for every Manager and Team Leader.
        List<Long> teamIds = employeeRepository.findSubtreeEmployeeIds(managerId);
        if (teamIds.isEmpty()) {
            return buildWorkbook(List.of(), List.of());
        }

        // Roster first so employees with zero activity still get a summary row.
        List<Employee> roster = employeeRepository.findAllByIdInWithManager(teamIds);
        List<DailyCommitment> rows = dailyCommitmentRepository
                .findByEmployeeIdsAndWorkDateBetween(teamIds, startDate, endDate);

        return buildWorkbook(roster, rows);
    }

    // ------------------------------------------------------------------ workbook

    /**
     * Builds the two-sheet workbook. Streams rows straight into an in-memory
     * XSSFWorkbook — fine up to ~10k rows; beyond that we would switch to
     * SXSSFWorkbook. Today's volumes are well under that.
     *
     * @param roster every employee in scope, including those with no rows in
     *               range — drives the summary sheet so silent employees are
     *               visible rather than absent
     * @param rows   the commitment rows in range, any status
     */
    private byte[] buildWorkbook(List<Employee> roster, List<DailyCommitment> rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            writeDetailSheet(workbook, styles, rows);
            writeSummarySheet(workbook, styles, roster, rows);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void writeDetailSheet(Workbook workbook, Styles styles, List<DailyCommitment> rows) {
        Sheet sheet = workbook.createSheet(DETAIL_SHEET);
        writeHeader(sheet, styles, DETAIL_COLUMNS);

        int rowIdx = 1;
        for (DailyCommitment c : rows) {
            Row row = sheet.createRow(rowIdx++);
            Employee emp = c.getEmployee();
            Employee reviewer = c.getApprovedBy();
            Employee supervisor = emp != null ? emp.getManager() : null;

            row.createCell(0).setCellValue(c.getWorkDate() != null ? c.getWorkDate().toString() : "");
            row.createCell(1).setCellValue(emp != null && emp.getEmployeeId() != null ? emp.getEmployeeId() : "");
            row.createCell(2).setCellValue(emp != null && emp.getFullName() != null ? emp.getFullName() : "");
            row.createCell(3).setCellValue(orZero(c.getTargetCalls()));
            row.createCell(4).setCellValue(orZero(c.getActualCalls()));
            row.createCell(5).setCellValue(orZero(c.getTargetOtps()));
            row.createCell(6).setCellValue(orZero(c.getActualOtps()));
            row.createCell(7).setCellValue(orZero(c.getTargetInterestedCustomers()));
            row.createCell(8).setCellValue(orZero(c.getActualInterestedCustomers()));
            money(row, 9, asDouble(c.getTargetDisbursalAmount()), styles);
            money(row, 10, asDouble(c.getActualDisbursalAmount()), styles);
            row.createCell(11).setCellValue(c.getStatus() != null ? c.getStatus().name() : "");
            row.createCell(12).setCellValue(c.getSubmittedAt() != null ? c.getSubmittedAt().toString() : "");
            row.createCell(13).setCellValue(reviewer != null && reviewer.getFullName() != null
                    ? reviewer.getFullName() : "");
            row.createCell(14).setCellValue(c.getNotes() != null ? c.getNotes() : "");
            row.createCell(15).setCellValue(supervisor != null && supervisor.getFullName() != null
                    ? supervisor.getFullName() : "");
        }

        autoSize(sheet, DETAIL_COLUMNS.length);
    }

    private void writeSummarySheet(Workbook workbook, Styles styles,
                                   List<Employee> roster, List<DailyCommitment> rows) {
        Sheet sheet = workbook.createSheet(SUMMARY_SHEET);
        writeHeader(sheet, styles, SUMMARY_COLUMNS);

        Map<Long, Agg> byEmployee = aggregate(roster, rows);

        // Group by supervisor so each TL/ATL branch reads as a block, then rank
        // within the branch by approved disbursal — the report is read top-down
        // to find who is carrying the number and who is not.
        List<Agg> ordered = new ArrayList<>(byEmployee.values());
        // Explicit lambda parameter types throughout — an implicitly typed
        // lambda makes thenComparing ambiguous between its Comparator and
        // key-extractor overloads.
        ordered.sort(Comparator
                .comparing((Agg a) -> a.supervisorName)
                .thenComparing(Comparator.comparing((Agg a) -> a.approvedDisbursal).reversed())
                .thenComparing((Agg a) -> a.employeeName));

        int rowIdx = 1;
        Agg grand = new Agg("", "TOTAL", "");
        for (Agg a : ordered) {
            Row row = sheet.createRow(rowIdx++);
            writeSummaryRow(row, a, styles, false);
            grand.absorb(a);
        }

        if (!ordered.isEmpty()) {
            // Blank spacer, then a bold grand total. The Approved Disbursal
            // total here is the number that must match the dashboard card.
            rowIdx++;
            writeSummaryRow(sheet.createRow(rowIdx), grand, styles, true);
        }

        autoSize(sheet, SUMMARY_COLUMNS.length);
    }

    private void writeSummaryRow(Row row, Agg a, Styles styles, boolean bold) {
        CellStyle text = bold ? styles.boldText : null;
        setStyled(row, 0, a.employeeCode, text);
        setStyled(row, 1, a.employeeName, text);
        setStyled(row, 2, a.supervisorName, text);
        setStyled(row, 3, a.daysReported, text);
        setStyled(row, 4, a.daysApproved, text);
        setStyled(row, 5, a.targetCalls, text);
        setStyled(row, 6, a.actualCalls, text);
        setStyled(row, 7, a.targetOtps, text);
        setStyled(row, 8, a.actualOtps, text);
        setStyled(row, 9, a.targetCustomers, text);
        setStyled(row, 10, a.actualCustomers, text);
        money(row, 11, a.targetDisbursal.doubleValue(), styles, bold);
        money(row, 12, a.approvedDisbursal.doubleValue(), styles, bold);

        Cell pct = row.createCell(13);
        pct.setCellValue(a.achievementPercent());
        pct.setCellStyle(bold ? styles.boldPercent : styles.percent);
    }

    /**
     * Folds commitment rows into one bucket per employee, seeded from the
     * roster so zero-activity employees survive into the output.
     */
    private Map<Long, Agg> aggregate(List<Employee> roster, List<DailyCommitment> rows) {
        Map<Long, Agg> map = new LinkedHashMap<>();
        for (Employee e : roster) {
            if (e == null || e.getId() == null) {
                continue;
            }
            map.put(e.getId(), new Agg(
                    nvl(e.getEmployeeId()),
                    nvl(e.getFullName()),
                    e.getManager() != null ? nvl(e.getManager().getFullName()) : ""));
        }

        for (DailyCommitment c : rows) {
            Employee emp = c.getEmployee();
            if (emp == null || emp.getId() == null) {
                continue;
            }
            // Defensive: a row whose employee somehow isn't in the roster still
            // gets counted rather than silently dropped.
            Agg a = map.computeIfAbsent(emp.getId(), id -> new Agg(
                    nvl(emp.getEmployeeId()),
                    nvl(emp.getFullName()),
                    emp.getManager() != null ? nvl(emp.getManager().getFullName()) : ""));
            a.add(c);
        }
        return map;
    }

    // ------------------------------------------------------------------ helpers

    private void writeHeader(Sheet sheet, Styles styles, String[] columns) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(styles.header);
        }
    }

    private void autoSize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void money(Row row, int col, double value, Styles styles) {
        money(row, col, value, styles, false);
    }

    private void money(Row row, int col, double value, Styles styles, boolean bold) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(bold ? styles.boldMoney : styles.money);
    }

    private void setStyled(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void setStyled(Row row, int col, long value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    private static int orZero(Integer value) {
        return value != null ? value : 0;
    }

    private static double asDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /** Per-employee accumulator for the summary sheet. */
    private static final class Agg {
        private final String employeeCode;
        private final String employeeName;
        private final String supervisorName;

        private long daysReported;
        private long daysApproved;
        private long targetCalls;
        private long actualCalls;
        private long targetOtps;
        private long actualOtps;
        private long targetCustomers;
        private long actualCustomers;
        private BigDecimal targetDisbursal = BigDecimal.ZERO;
        /** APPROVED rows only — this is what reconciles with the dashboard. */
        private BigDecimal approvedDisbursal = BigDecimal.ZERO;

        private Agg(String employeeCode, String employeeName, String supervisorName) {
            this.employeeCode = employeeCode;
            this.employeeName = employeeName;
            this.supervisorName = supervisorName;
        }

        private void add(DailyCommitment c) {
            daysReported++;
            targetCalls += orZero(c.getTargetCalls());
            actualCalls += orZero(c.getActualCalls());
            targetOtps += orZero(c.getTargetOtps());
            actualOtps += orZero(c.getActualOtps());
            targetCustomers += orZero(c.getTargetInterestedCustomers());
            actualCustomers += orZero(c.getActualInterestedCustomers());
            targetDisbursal = targetDisbursal.add(nz(c.getTargetDisbursalAmount()));
            if (c.getStatus() == CommitmentStatus.APPROVED) {
                daysApproved++;
                approvedDisbursal = approvedDisbursal.add(nz(c.getActualDisbursalAmount()));
            }
        }

        private void absorb(Agg other) {
            daysReported += other.daysReported;
            daysApproved += other.daysApproved;
            targetCalls += other.targetCalls;
            actualCalls += other.actualCalls;
            targetOtps += other.targetOtps;
            actualOtps += other.actualOtps;
            targetCustomers += other.targetCustomers;
            actualCustomers += other.actualCustomers;
            targetDisbursal = targetDisbursal.add(other.targetDisbursal);
            approvedDisbursal = approvedDisbursal.add(other.approvedDisbursal);
        }

        /**
         * Approved disbursal as a percentage of committed target. Not capped at
         * 100 — over-achievement is information worth seeing, unlike the
         * dashboard card which clamps for progress-bar rendering.
         */
        private double achievementPercent() {
            if (targetDisbursal.signum() <= 0) {
                return 0d;
            }
            return approvedDisbursal
                    .multiply(BigDecimal.valueOf(100))
                    .divide(targetDisbursal, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }
    }

    /** Cell styles, created once per workbook — POI caps the style pool. */
    private static final class Styles {
        private final CellStyle header;
        private final CellStyle money;
        private final CellStyle boldMoney;
        private final CellStyle percent;
        private final CellStyle boldPercent;
        private final CellStyle boldText;

        private Styles(Workbook workbook) {
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);

            DataFormat formats = workbook.createDataFormat();
            short moneyFormat = formats.getFormat("#,##0.00");
            short percentFormat = formats.getFormat("0.0");

            header = workbook.createCellStyle();
            header.setFont(boldFont);
            header.setBorderBottom(BorderStyle.THIN);

            money = workbook.createCellStyle();
            money.setDataFormat(moneyFormat);

            boldMoney = workbook.createCellStyle();
            boldMoney.setDataFormat(moneyFormat);
            boldMoney.setFont(boldFont);
            boldMoney.setBorderTop(BorderStyle.THIN);

            percent = workbook.createCellStyle();
            percent.setDataFormat(percentFormat);

            boldPercent = workbook.createCellStyle();
            boldPercent.setDataFormat(percentFormat);
            boldPercent.setFont(boldFont);
            boldPercent.setBorderTop(BorderStyle.THIN);

            boldText = workbook.createCellStyle();
            boldText.setFont(boldFont);
            boldText.setBorderTop(BorderStyle.THIN);
        }
    }
}
