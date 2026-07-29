package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.dto.payroll.PayrollGenerateRequest;
import com.financebuddha.finbud.hrms.dto.payroll.PayrollResponse;
import com.financebuddha.finbud.hrms.dto.payroll.PayrollSummaryResponse;
import com.financebuddha.finbud.hrms.enums.PayrollStatus;

import java.util.List;

public interface PayrollService {

    /**
     * Convenience overload kept for callers that have only the three
     * identifiers (employeeId / month / year) and no manual overrides.
     * Delegates to the rich {@link #generatePayroll(PayrollGenerateRequest)}.
     */
    PayrollResponse generatePayroll(Long employeeId, Integer month, Integer year);

    /**
     * Generate a single-employee payroll with full control:
     * manual LOP, incentives override, adjustments, reason. The request's
     * {@code employeeId} is required for this flow.
     */
    PayrollResponse generatePayroll(PayrollGenerateRequest request);

    List<PayrollResponse> generatePayrollForAll(PayrollGenerateRequest request);

    PayrollResponse getPayrollById(Long id);

    PayrollResponse getPayrollByEmployeeAndMonth(Long employeeId, Integer month, Integer year);

    PagedResponse<PayrollResponse> getPayrollsByEmployee(Long employeeId, PaginationRequest paginationRequest);

    PagedResponse<PayrollResponse> getPayrollsByMonthAndYear(Integer month, Integer year, PaginationRequest paginationRequest);

    PagedResponse<PayrollResponse> getPayrollsByStatus(PayrollStatus status, PaginationRequest paginationRequest);

    PayrollResponse approvePayroll(Long payrollId, Long approverId);

    PayrollResponse markAsPaid(Long payrollId);

    PayrollSummaryResponse getPayrollSummary(Integer month, Integer year);

    byte[] generatePayslipPdf(Long payrollId);

    void autoGenerateMonthlyPayroll();
}
