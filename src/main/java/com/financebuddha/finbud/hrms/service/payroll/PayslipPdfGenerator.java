package com.financebuddha.finbud.hrms.service.payroll;

import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.Payroll;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Produces a standard Indian payslip PDF for a given {@link Payroll}.
 * <p>
 * The layout follows the template familiar to Indian employees: company
 * header, employee information grid, an earnings/deductions table, net pay
 * in figures and in words, followed by a system-generated footer. No
 * signature block — the portal's audit trail is the authoritative record.
 */
@Slf4j
@Component
public class PayslipPdfGenerator {

    // Finbud brand palette — kept in one place so any future rebrand
    // touches a single file.
    private static final Color BRAND_DARK = new Color(15, 23, 42);     // slate-900
    private static final Color BRAND_LIGHT = new Color(241, 245, 249); // slate-100
    private static final Color BRAND_BORDER = new Color(203, 213, 225);// slate-300

    private static final Font H1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BRAND_DARK);
    private static final Font H2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BRAND_DARK);
    private static final Font LABEL = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
    private static final Font VALUE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, BRAND_DARK);
    private static final Font TABLE_HEAD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font TABLE_CELL = FontFactory.getFont(FontFactory.HELVETICA, 10, BRAND_DARK);
    private static final Font TABLE_CELL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BRAND_DARK);
    private static final Font FOOTER = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.DARK_GRAY);

    public byte[] generate(Payroll payroll) {
        if (payroll == null) {
            throw new IllegalArgumentException("payroll must not be null");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(buildHeader(payroll));
            document.add(spacer(8));
            document.add(buildEmployeeInfo(payroll));
            document.add(spacer(12));
            document.add(buildEarningsDeductionsTable(payroll));
            document.add(spacer(8));
            document.add(buildNetPayBlock(payroll));
            document.add(spacer(18));
            document.add(buildFooter());

            document.close();
        } catch (Exception ex) {
            log.error("Failed to generate payslip PDF for payroll {}: {}",
                    payroll.getId(), ex.getMessage(), ex);
            throw new IllegalStateException("Failed to generate payslip PDF", ex);
        }

        return out.toByteArray();
    }

    // ------------------------------------------------------------------
    // Layout sections
    // ------------------------------------------------------------------

    private Paragraph buildHeader(Payroll payroll) {
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk("Finbud Financial", H1));
        p.add(Chunk.NEWLINE);
        p.add(new Chunk("B-77, Sector 60, Noida, Uttar Pradesh 201301, India",
                FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY)));
        p.add(Chunk.NEWLINE);
        p.add(new Chunk("Payslip for " + monthLabel(payroll), H2));
        return p;
    }

    private PdfPTable buildEmployeeInfo(Payroll payroll) {
        Employee employee = payroll.getEmployee();
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100f);
        try {
            table.setWidths(new float[]{1.1f, 2f, 1.1f, 2f});
        } catch (Exception ignored) { /* widths fail only on zero-length arrays */ }
        table.setSpacingBefore(6f);

        addKv(table, "Employee Code", nn(employee != null ? employee.getEmployeeId() : null));
        addKv(table, "Employee Name", fullName(employee));
        addKv(table, "Designation",  nn(employee != null ? employee.getDesignation() : null));
        addKv(table, "Department",   employee != null && employee.getDepartment() != null
                ? nn(employee.getDepartment().getName()) : "-");
        addKv(table, "Date of Joining", employee != null && employee.getDateOfJoining() != null
                ? employee.getDateOfJoining().toString() : "-");
        addKv(table, "PAN",            nn(employee != null ? employee.getPanNumber() : null));
        addKv(table, "UAN",            nn(employee != null ? employee.getUanNumber() : null));
        addKv(table, "PF Number",      nn(employee != null ? employee.getPfNumber() : null));
        addKv(table, "ESI Number",     nn(employee != null ? employee.getEsiNumber() : null));
        addKv(table, "Aadhaar (masked)", maskAadhaar(employee != null ? employee.getAadhaarNumber() : null));
        addKv(table, "Bank Name",      nn(employee != null ? employee.getBankName() : null));
        addKv(table, "Bank A/C",       nn(employee != null ? employee.getBankAccountNumber() : null));
        addKv(table, "IFSC",           nn(employee != null ? employee.getBankIfscCode() : null));
        addKv(table, "Payment Mode",   "Bank Transfer");
        addKv(table, "Working Days",   str(payroll.getTotalWorkingDays()));
        addKv(table, "LOP Days",       str(payroll.getLopDays()));

        return table;
    }

    private PdfPTable buildEarningsDeductionsTable(Payroll payroll) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100f);
        try {
            table.setWidths(new float[]{2f, 1.2f, 2f, 1.2f});
        } catch (Exception ignored) { /* widths fail only on zero-length arrays */ }

        // Header row
        table.addCell(headerCell("Earnings"));
        table.addCell(headerCell("Amount (INR)"));
        table.addCell(headerCell("Deductions"));
        table.addCell(headerCell("Amount (INR)"));

        String[][] earnings = earningsRows(payroll);
        String[][] deductions = deductionsRows(payroll);
        int rows = Math.max(earnings.length, deductions.length);

        for (int i = 0; i < rows; i++) {
            if (i < earnings.length) {
                table.addCell(labelCell(earnings[i][0]));
                table.addCell(amountCell(earnings[i][1]));
            } else {
                table.addCell(labelCell(""));
                table.addCell(amountCell(""));
            }
            if (i < deductions.length) {
                table.addCell(labelCell(deductions[i][0]));
                table.addCell(amountCell(deductions[i][1]));
            } else {
                table.addCell(labelCell(""));
                table.addCell(amountCell(""));
            }
        }

        // Totals row
        table.addCell(totalsLabelCell("Gross Earnings"));
        table.addCell(totalsAmountCell(money(payroll.getGrossEarnings())));
        table.addCell(totalsLabelCell("Total Deductions"));
        table.addCell(totalsAmountCell(money(payroll.getTotalDeductions())));

        return table;
    }

    private String[][] earningsRows(Payroll payroll) {
        // Prefer the CTC view when the structure is CTC-based (monthlyGrossCtc
        // present); fall back to component view for legacy records.
        if (payroll.getMonthlyGrossCtc() != null && payroll.getMonthlyGrossCtc().signum() > 0) {
            return new String[][]{
                    {"Monthly Gross CTC", money(payroll.getMonthlyGrossCtc())},
                    {"Incentives",        money(payroll.getIncentiveAmount())},
                    {"Overtime Pay",      money(payroll.getOvertimePay())},
                    {"Adjustments",       money(payroll.getAdjustments())},
            };
        }
        return new String[][]{
                {"Basic",                   money(payroll.getBasicEarned())},
                {"HRA",                     money(payroll.getHraEarned())},
                {"Dearness Allowance",      money(payroll.getDaEarned())},
                {"Conveyance",              money(payroll.getConveyanceEarned())},
                {"Medical",                 money(payroll.getMedicalEarned())},
                {"Special Allowance",       money(payroll.getSpecialEarned())},
                {"Overtime Pay",            money(payroll.getOvertimePay())},
        };
    }

    private String[][] deductionsRows(Payroll payroll) {
        if (payroll.getMonthlyGrossCtc() != null && payroll.getMonthlyGrossCtc().signum() > 0) {
            return new String[][]{
                    {"Employee PF",       money(payroll.getEmployeePf())},
                    {"Employee ESI",      money(payroll.getEmployeeEsi())},
                    {"LWF",               money(payroll.getLwfAmount())},
                    {"TDS",               money(payroll.getTdsAmount())},
                    {"LOP Deduction",     money(payroll.getLopDeduction())},
                    {"Other Deductions",  money(payroll.getOtherDeductions())},
            };
        }
        return new String[][]{
                {"PF",                money(payroll.getPfDeduction())},
                {"ESI",               money(payroll.getEsiDeduction())},
                {"Professional Tax",  money(payroll.getPtDeduction())},
                {"LOP Deduction",     money(payroll.getLopDeduction())},
                {"Other Deductions",  money(payroll.getOtherDeductions())},
        };
    }

    private PdfPTable buildNetPayBlock(Payroll payroll) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100f);
        try {
            table.setWidths(new float[]{1f, 1.5f});
        } catch (Exception ignored) { /* widths fail only on zero-length arrays */ }

        PdfPCell net = new PdfPCell(new Phrase("Net Pay", TABLE_HEAD));
        net.setBackgroundColor(BRAND_DARK);
        net.setPadding(8f);
        net.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(net);

        PdfPCell amt = new PdfPCell(new Phrase("INR " + money(payroll.getNetPay()), TABLE_HEAD));
        amt.setBackgroundColor(BRAND_DARK);
        amt.setPadding(8f);
        amt.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(amt);

        PdfPCell wordsLabel = new PdfPCell(new Phrase("Net Pay in Words", LABEL));
        wordsLabel.setBackgroundColor(BRAND_LIGHT);
        wordsLabel.setPadding(6f);
        table.addCell(wordsLabel);

        PdfPCell words = new PdfPCell(new Phrase(amountInWords(payroll.getNetPay()), VALUE));
        words.setBackgroundColor(BRAND_LIGHT);
        words.setPadding(6f);
        words.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(words);

        if (payroll.getAdjustmentReason() != null && !payroll.getAdjustmentReason().isBlank()) {
            PdfPCell adjLabel = new PdfPCell(new Phrase("Adjustment Reason", LABEL));
            adjLabel.setPadding(6f);
            adjLabel.setBorderColor(BRAND_BORDER);
            table.addCell(adjLabel);

            PdfPCell adj = new PdfPCell(new Phrase(payroll.getAdjustmentReason(), VALUE));
            adj.setPadding(6f);
            adj.setHorizontalAlignment(Element.ALIGN_RIGHT);
            adj.setBorderColor(BRAND_BORDER);
            table.addCell(adj);
        }

        return table;
    }

    private Paragraph buildFooter() {
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk("This is a computer-generated payslip and does not require a signature.", FOOTER));
        p.add(Chunk.NEWLINE);
        p.add(new Chunk("For payroll queries please contact the HR desk at hr@finbud.co.in.", FOOTER));
        return p;
    }

    // ------------------------------------------------------------------
    // Cell builders
    // ------------------------------------------------------------------

    private void addKv(PdfPTable table, String label, String value) {
        PdfPCell l = new PdfPCell(new Phrase(label, LABEL));
        l.setBackgroundColor(BRAND_LIGHT);
        l.setBorderColor(BRAND_BORDER);
        l.setPadding(5f);
        table.addCell(l);

        PdfPCell v = new PdfPCell(new Phrase(value, VALUE));
        v.setBorderColor(BRAND_BORDER);
        v.setPadding(5f);
        table.addCell(v);
    }

    private PdfPCell headerCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, TABLE_HEAD));
        c.setBackgroundColor(BRAND_DARK);
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(BRAND_DARK);
        c.setPadding(6f);
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        return c;
    }

    private PdfPCell labelCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, TABLE_CELL));
        c.setBorderColor(BRAND_BORDER);
        c.setPadding(5f);
        return c;
    }

    private PdfPCell amountCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, TABLE_CELL));
        c.setBorderColor(BRAND_BORDER);
        c.setPadding(5f);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    private PdfPCell totalsLabelCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, TABLE_CELL_BOLD));
        c.setBackgroundColor(BRAND_LIGHT);
        c.setBorderColor(BRAND_BORDER);
        c.setPadding(6f);
        return c;
    }

    private PdfPCell totalsAmountCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, TABLE_CELL_BOLD));
        c.setBackgroundColor(BRAND_LIGHT);
        c.setBorderColor(BRAND_BORDER);
        c.setPadding(6f);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return c;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static Paragraph spacer(float leading) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(leading);
        return p;
    }

    private static String fullName(Employee e) {
        if (e == null) return "-";
        String first = e.getFirstName() == null ? "" : e.getFirstName();
        String last = e.getLastName() == null ? "" : e.getLastName();
        String full = (first + " " + last).trim();
        return full.isEmpty() ? "-" : full;
    }

    private static String monthLabel(Payroll p) {
        if (p.getMonth() == null || p.getYear() == null) return "-";
        YearMonth ym = YearMonth.of(p.getYear(), p.getMonth());
        return ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + ym.getYear();
    }

    /** Mask all-but-last-4 for Aadhaar to avoid leaking a full UIDAI number on a printable. */
    private static String maskAadhaar(String raw) {
        if (raw == null || raw.isBlank()) return "-";
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() < 4) return raw;
        String last4 = digits.substring(digits.length() - 4);
        return "XXXX XXXX " + last4;
    }

    private static String nn(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private static String str(Integer v) {
        return v == null ? "-" : String.valueOf(v);
    }

    private static String str(BigDecimal v) {
        return v == null ? "-" : v.stripTrailingZeros().toPlainString();
    }

    private static String money(BigDecimal v) {
        if (v == null) return "0.00";
        return String.format(Locale.ENGLISH, "%,.2f", v);
    }

    /**
     * Convert an INR amount to words in the Indian numbering system (lakh/crore).
     * Handles up to 99,99,99,99,999 — well beyond any realistic monthly payslip.
     */
    static String amountInWords(BigDecimal amount) {
        if (amount == null) return "Zero Rupees Only";
        BigDecimal abs = amount.abs();
        long rupees = abs.longValue();
        int paise = abs.subtract(BigDecimal.valueOf(rupees))
                .movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).intValue();

        StringBuilder sb = new StringBuilder();
        if (amount.signum() < 0) sb.append("Minus ");
        sb.append(inWordsIndian(rupees)).append(" Rupees");
        if (paise > 0) {
            sb.append(" and ").append(inWordsIndian(paise)).append(" Paise");
        }
        sb.append(" Only");
        return sb.toString();
    }

    private static final String[] ONES = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private static String twoDigit(long n) {
        if (n < 20) return ONES[(int) n];
        return (TENS[(int) (n / 10)] + (n % 10 > 0 ? " " + ONES[(int) (n % 10)] : "")).trim();
    }

    private static String threeDigit(long n) {
        if (n < 100) return twoDigit(n);
        long hundreds = n / 100;
        long rest = n % 100;
        String h = ONES[(int) hundreds] + " Hundred";
        return rest == 0 ? h : h + " " + twoDigit(rest);
    }

    private static String inWordsIndian(long n) {
        if (n == 0) return "Zero";
        StringBuilder sb = new StringBuilder();
        long crore = n / 10_000_000L; n %= 10_000_000L;
        long lakh  = n / 100_000L;    n %= 100_000L;
        long thousand = n / 1000L;    n %= 1000L;
        if (crore > 0)    sb.append(twoDigit(crore)).append(" Crore ");
        if (lakh > 0)     sb.append(twoDigit(lakh)).append(" Lakh ");
        if (thousand > 0) sb.append(twoDigit(thousand)).append(" Thousand ");
        if (n > 0)        sb.append(threeDigit(n));
        return sb.toString().trim().replaceAll("\\s+", " ");
    }
}
