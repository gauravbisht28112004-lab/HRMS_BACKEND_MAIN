package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.security.AuthzService;
import com.financebuddha.finbud.hrms.service.CommitmentReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Excel report endpoints — Q1 Phase D Part 2. Streams an .xlsx as raw
 * bytes; the frontend handles the download via blob → ObjectURL the same
 * way the payslip download works (PayrollController#generatePayslipPdf).
 *
 * <p>Authorisation. The role annotation is only the coarse gate — every
 * supervisor carries the same role, so it cannot by itself stop one manager
 * reading another manager's team. Each method therefore also calls an
 * {@code AuthzService} guard that checks the caller's position in the
 * reporting tree:
 * <ul>
 *   <li>Employee report ({@code /employee/{id}}) — ADMIN/HR anyone; a
 *       MANAGER/TEAM_LEADER/ATL only for an employee inside their own
 *       subtree; the employee themselves for their own record.</li>
 *   <li>Team report ({@code /team/{managerId}}) — ADMIN/HR any node; a
 *       supervisor only their own subtree, which by the single-parent tree
 *       property can never include another manager's TLs or ATLs.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/reports/commitment")
@RequiredArgsConstructor
@Tag(name = "Commitment Reports", description = "Daily commitment Excel exports")
@SecurityRequirement(name = "bearerAuth")
public class CommitmentReportController {

    private final CommitmentReportService commitmentReportService;
    private final AuthzService authzService;

    @GetMapping("/employee/{employeeId}.xlsx")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER', 'TEAM_LEADER', 'ATL') or @authz.isOwner(#employeeId)")
    @Operation(summary = "Daily commitment report for one employee (Excel)",
               description = "Supervisors may only export an employee inside their own reporting subtree.")
    public ResponseEntity<byte[]> employeeReport(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {
        authzService.requireCanExportEmployeeReport(employeeId);
        byte[] bytes = commitmentReportService.employeeDailyXlsx(employeeId, startDate, endDate);
        String filename = "commitments-employee-%d-%s-to-%s.xlsx".formatted(employeeId, startDate, endDate);
        return excelResponse(filename, bytes);
    }

    @GetMapping("/team/{managerId}.xlsx")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER', 'TEAM_LEADER', 'ATL')")
    @Operation(summary = "Daily commitment report for a supervisor's whole team (Excel)",
               description = "Covers every employee in the caller's reporting subtree — a Manager gets the "
                       + "employees under all their Team Leaders and ATLs. Supervisors may only export "
                       + "their own subtree; ADMIN/HR may export any.")
    public ResponseEntity<byte[]> teamReport(
            @PathVariable Long managerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {
        authzService.requireCanExportTeamReport(managerId);
        byte[] bytes = commitmentReportService.teamDailyXlsx(managerId, startDate, endDate);
        String filename = "commitments-team-%d-%s-to-%s.xlsx".formatted(managerId, startDate, endDate);
        return excelResponse(filename, bytes);
    }

    /** Common excel-stream response with the right MIME + Content-Disposition. */
    private ResponseEntity<byte[]> excelResponse(String filename, byte[] bytes) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(org.springframework.http.ContentDisposition
                .attachment().filename(filename).build());
        headers.setContentLength(bytes.length);
        return new ResponseEntity<>(bytes, headers, org.springframework.http.HttpStatus.OK);
    }
}
