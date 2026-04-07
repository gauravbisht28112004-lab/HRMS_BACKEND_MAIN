package com.financebuddha.finbud.hrms.controller;

import com.financebuddha.finbud.hrms.dto.common.ApiResponse;
import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.dto.payroll.PayrollGenerateRequest;
import com.financebuddha.finbud.hrms.dto.payroll.PayrollResponse;
import com.financebuddha.finbud.hrms.dto.payroll.PayrollSummaryResponse;
import com.financebuddha.finbud.hrms.enums.PayrollStatus;
import com.financebuddha.finbud.hrms.security.CurrentUser;
import com.financebuddha.finbud.hrms.security.UserPrincipal;
import com.financebuddha.finbud.hrms.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
@Tag(name = "Payroll", description = "Payroll management APIs")
@SecurityRequirement(name = "bearerAuth")
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Generate payroll", description = "Generate payroll for employee")
    public ResponseEntity<ApiResponse<PayrollResponse>> generatePayroll(
            @RequestParam Long employeeId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        PayrollResponse response = payrollService.generatePayroll(employeeId, month, year);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Payroll generated successfully", response));
    }

    @PostMapping("/generate-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Generate payroll for all", description = "Generate payroll for all employees")
    public ResponseEntity<ApiResponse<List<PayrollResponse>>> generatePayrollForAll(
            @Valid @RequestBody PayrollGenerateRequest request) {
        List<PayrollResponse> response = payrollService.generatePayrollForAll(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Payroll generated for all employees", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Get payroll by ID", description = "Get payroll details by ID")
    public ResponseEntity<ApiResponse<PayrollResponse>> getPayrollById(@PathVariable Long id) {
        PayrollResponse response = payrollService.getPayrollById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get employee payroll", description = "Get payroll history for employee")
    public ResponseEntity<ApiResponse<PagedResponse<PayrollResponse>>> getPayrollsByEmployee(
            @PathVariable Long employeeId,
            @ParameterObject PaginationRequest paginationRequest) {
        PagedResponse<PayrollResponse> response = payrollService.getPayrollsByEmployee(employeeId, paginationRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/month/{month}/year/{year}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Get payroll by month", description = "Get payroll for specific month and year")
    public ResponseEntity<ApiResponse<PagedResponse<PayrollResponse>>> getPayrollsByMonthAndYear(
            @PathVariable Integer month,
            @PathVariable Integer year,
            @ParameterObject PaginationRequest paginationRequest) {
        PagedResponse<PayrollResponse> response = payrollService.getPayrollsByMonthAndYear(month, year, paginationRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Get payroll by status", description = "Get payroll by status")
    public ResponseEntity<ApiResponse<PagedResponse<PayrollResponse>>> getPayrollsByStatus(
            @PathVariable PayrollStatus status,
            @ParameterObject PaginationRequest paginationRequest) {
        PagedResponse<PayrollResponse> response = payrollService.getPayrollsByStatus(status, paginationRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{payrollId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Approve payroll", description = "Approve generated payroll")
    public ResponseEntity<ApiResponse<PayrollResponse>> approvePayroll(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long payrollId) {
        PayrollResponse response = payrollService.approvePayroll(payrollId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Payroll approved", response));
    }

    @PostMapping("/{payrollId}/mark-paid")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Mark as paid", description = "Mark payroll as paid")
    public ResponseEntity<ApiResponse<PayrollResponse>> markAsPaid(@PathVariable Long payrollId) {
        PayrollResponse response = payrollService.markAsPaid(payrollId);
        return ResponseEntity.ok(ApiResponse.success("Payroll marked as paid", response));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Get payroll summary", description = "Get monthly payroll summary")
    public ResponseEntity<ApiResponse<PayrollSummaryResponse>> getPayrollSummary(
            @RequestParam Integer month,
            @RequestParam Integer year) {
        PayrollSummaryResponse response = payrollService.getPayrollSummary(month, year);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{payrollId}/payslip")
    @Operation(summary = "Generate payslip", description = "Generate payslip PDF")
    public ResponseEntity<ApiResponse<byte[]>> generatePayslipPdf(@PathVariable Long payrollId) {
        byte[] pdf = payrollService.generatePayslipPdf(payrollId);
        return ResponseEntity.ok(ApiResponse.success(pdf));
    }
}
