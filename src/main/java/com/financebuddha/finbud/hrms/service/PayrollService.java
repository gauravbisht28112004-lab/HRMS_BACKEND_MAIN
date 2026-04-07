package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.dto.payroll.PayrollGenerateRequest;
import com.financebuddha.finbud.hrms.dto.payroll.PayrollResponse;
import com.financebuddha.finbud.hrms.dto.payroll.PayrollSummaryResponse;
import com.financebuddha.finbud.hrms.enums.PayrollStatus;

import java.util.List;

public interface PayrollService {

    PayrollResponse generatePayroll(Long employeeId, Integer month, Integer year);

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
