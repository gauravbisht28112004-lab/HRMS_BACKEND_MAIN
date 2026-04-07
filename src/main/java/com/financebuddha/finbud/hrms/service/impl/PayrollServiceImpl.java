package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.dto.common.PagedResponse;
import com.financebuddha.finbud.hrms.dto.common.PaginationRequest;
import com.financebuddha.finbud.hrms.dto.payroll.PayrollGenerateRequest;
import com.financebuddha.finbud.hrms.dto.payroll.PayrollResponse;
import com.financebuddha.finbud.hrms.dto.payroll.PayrollSummaryResponse;
import com.financebuddha.finbud.hrms.entity.Attendance;
import com.financebuddha.finbud.hrms.entity.Employee;
import com.financebuddha.finbud.hrms.entity.Payroll;
import com.financebuddha.finbud.hrms.entity.SalaryStructure;
import com.financebuddha.finbud.hrms.enums.AttendanceStatus;
import com.financebuddha.finbud.hrms.enums.EmployeeStatus;
import com.financebuddha.finbud.hrms.enums.PayrollStatus;
import com.financebuddha.finbud.hrms.exception.BadRequestException;
import com.financebuddha.finbud.hrms.exception.ResourceNotFoundException;
import com.financebuddha.finbud.hrms.mapper.PayrollMapper;
import com.financebuddha.finbud.hrms.repository.AttendanceRepository;
import com.financebuddha.finbud.hrms.repository.EmployeeRepository;
import com.financebuddha.finbud.hrms.repository.PayrollRepository;
import com.financebuddha.finbud.hrms.repository.SalaryStructureRepository;
import com.financebuddha.finbud.hrms.service.PayrollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final PayrollMapper payrollMapper;

    @Override
    @Transactional
    public PayrollResponse generatePayroll(Long employeeId, Integer month, Integer year) {
        log.info("Generating payroll for employee {} for {}/{}", employeeId, month, year);

        if (payrollRepository.existsByEmployeeIdAndMonthAndYear(employeeId, month, year)) {
            throw new BadRequestException("Payroll already exists for this employee for " + month + "/" + year);
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        SalaryStructure salary = salaryStructureRepository.findByEmployeeIdAndIsActiveTrue(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("SalaryStructure", "employeeId", employeeId));

        // Calculate month dates
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.with(TemporalAdjusters.lastDayOfMonth());

        // Get attendance data
        List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(
                employeeId, startOfMonth, endOfMonth);

        Payroll payroll = calculatePayroll(employee, salary, attendances, month, year);
        payroll.setGeneratedAt(LocalDateTime.now());

        Payroll savedPayroll = payrollRepository.save(payroll);
        return payrollMapper.toResponse(savedPayroll);
    }

    @Override
    @Transactional
    public List<PayrollResponse> generatePayrollForAll(PayrollGenerateRequest request) {
        log.info("Generating payroll for all employees for {}/{}", request.getMonth(), request.getYear());

        List<Employee> employees;
        if (request.getEmployeeId() != null) {
            employees = List.of(employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId())));
        } else {
            employees = employeeRepository.findByStatus(EmployeeStatus.ACTIVE, Pageable.unpaged()).getContent();
        }

        List<PayrollResponse> results = new ArrayList<>();
        for (Employee employee : employees) {
            try {
                PayrollResponse response = generatePayroll(employee.getId(), request.getMonth(), request.getYear());
                results.add(response);
            } catch (Exception e) {
                log.error("Failed to generate payroll for employee {}: {}", employee.getId(), e.getMessage());
            }
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollResponse getPayrollById(Long id) {
        Payroll payroll = payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll", "id", id));
        return payrollMapper.toResponse(payroll);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollResponse getPayrollByEmployeeAndMonth(Long employeeId, Integer month, Integer year) {
        Payroll payroll = payrollRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll", "month/year", month + "/" + year));
        return payrollMapper.toResponse(payroll);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PayrollResponse> getPayrollsByEmployee(Long employeeId, PaginationRequest paginationRequest) {
        Pageable pageable = createPageable(paginationRequest);
        Page<Payroll> payrollPage = payrollRepository.findAll(pageable); // Simplified - should filter by employee

        return PagedResponse.of(
                payrollMapper.toResponseList(payrollPage.getContent()),
                payrollPage.getNumber(),
                payrollPage.getSize(),
                payrollPage.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PayrollResponse> getPayrollsByMonthAndYear(Integer month, Integer year, PaginationRequest paginationRequest) {
        Pageable pageable = createPageable(paginationRequest);
        Page<Payroll> payrollPage = payrollRepository.findByMonthAndYear(month, year, pageable);

        return PagedResponse.of(
                payrollMapper.toResponseList(payrollPage.getContent()),
                payrollPage.getNumber(),
                payrollPage.getSize(),
                payrollPage.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<PayrollResponse> getPayrollsByStatus(PayrollStatus status, PaginationRequest paginationRequest) {
        Pageable pageable = createPageable(paginationRequest);
        Page<Payroll> payrollPage = payrollRepository.findByStatus(status, pageable);

        return PagedResponse.of(
                payrollMapper.toResponseList(payrollPage.getContent()),
                payrollPage.getNumber(),
                payrollPage.getSize(),
                payrollPage.getTotalElements()
        );
    }

    @Override
    @Transactional
    public PayrollResponse approvePayroll(Long payrollId, Long approverId) {
        log.info("Approving payroll: {}", payrollId);

        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll", "id", payrollId));

        Employee approver = employeeRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", approverId));

        payroll.setStatus(PayrollStatus.APPROVED);
        payroll.setApprovedBy(approver);
        payroll.setApprovedAt(LocalDateTime.now());

        Payroll updatedPayroll = payrollRepository.save(payroll);
        return payrollMapper.toResponse(updatedPayroll);
    }

    @Override
    @Transactional
    public PayrollResponse markAsPaid(Long payrollId) {
        log.info("Marking payroll as paid: {}", payrollId);

        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll", "id", payrollId));

        payroll.setStatus(PayrollStatus.PAID);
        payroll.setPaidAt(LocalDateTime.now());

        Payroll updatedPayroll = payrollRepository.save(payroll);
        return payrollMapper.toResponse(updatedPayroll);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollSummaryResponse getPayrollSummary(Integer month, Integer year) {
        Double totalGross = payrollRepository.sumGrossEarningsByMonthAndYear(month, year);
        Double totalDeductions = payrollRepository.sumTotalDeductionsByMonthAndYear(month, year);
        Double totalNetPay = payrollRepository.sumNetPayByMonthAndYear(month, year);
        Long totalEmployees = payrollRepository.count();

        return PayrollSummaryResponse.builder()
                .month(month)
                .year(year)
                .totalEmployees(totalEmployees != null ? totalEmployees : 0)
                .totalGrossEarnings(BigDecimal.valueOf(totalGross != null ? totalGross : 0))
                .totalDeductions(BigDecimal.valueOf(totalDeductions != null ? totalDeductions : 0))
                .totalNetPay(BigDecimal.valueOf(totalNetPay != null ? totalNetPay : 0))
                .totalOvertimePay(BigDecimal.ZERO)
                .paidCount(0L)
                .pendingCount(totalEmployees != null ? totalEmployees : 0)
                .build();
    }

    @Override
    public byte[] generatePayslipPdf(Long payrollId) {
        // TODO: Implement PDF generation using OpenPDF
        log.info("Generating payslip PDF for payroll: {}", payrollId);
        return new byte[0];
    }

    @Override
    @Transactional
    public void autoGenerateMonthlyPayroll() {
        log.info("Auto-generating monthly payroll");
        LocalDate now = LocalDate.now().minusMonths(1);

        PayrollGenerateRequest request = new PayrollGenerateRequest();
        request.setMonth(now.getMonthValue());
        request.setYear(now.getYear());

        generatePayrollForAll(request);
    }

    private Payroll calculatePayroll(Employee employee, SalaryStructure salary, List<Attendance> attendances,
                                     Integer month, Integer year) {

        int totalWorkingDays = calculateWorkingDays(month, year);

        // Count present days and other metrics
        BigDecimal presentDays = BigDecimal.ZERO;
        BigDecimal halfDays = BigDecimal.ZERO;
        BigDecimal overtimeHours = BigDecimal.ZERO;
        int weeklyOffDays = 0;

        for (Attendance attendance : attendances) {
            if (attendance.getStatus() == AttendanceStatus.PRESENT) {
                if (Boolean.TRUE.equals(attendance.getIsHalfDay())) {
                    halfDays = halfDays.add(new BigDecimal("0.5"));
                    presentDays = presentDays.add(new BigDecimal("0.5"));
                } else {
                    presentDays = presentDays.add(BigDecimal.ONE);
                }

                if (attendance.getOvertimeHours() != null) {
                    overtimeHours = overtimeHours.add(attendance.getOvertimeHours());
                }
            } else if (attendance.getStatus() == AttendanceStatus.WEEKLY_OFF) {
                weeklyOffDays++;
            }
        }

        // Calculate attendance ratio
        BigDecimal effectiveWorkingDays = presentDays.add(halfDays.multiply(new BigDecimal("0.5")));
        BigDecimal attendanceRatio = totalWorkingDays > 0 ?
                effectiveWorkingDays.divide(BigDecimal.valueOf(totalWorkingDays), 4, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        // Calculate earnings
        BigDecimal basicEarned = salary.getBasicSalary().multiply(attendanceRatio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal hraEarned = salary.getHra().multiply(attendanceRatio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal daEarned = salary.getDa().multiply(attendanceRatio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal conveyanceEarned = salary.getConveyanceAllowance().multiply(attendanceRatio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal medicalEarned = salary.getMedicalAllowance().multiply(attendanceRatio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal specialEarned = salary.getSpecialAllowance().multiply(attendanceRatio).setScale(2, RoundingMode.HALF_UP);

        BigDecimal grossEarnings = basicEarned.add(hraEarned).add(daEarned)
                .add(conveyanceEarned).add(medicalEarned).add(specialEarned);

        // Calculate deductions
        BigDecimal pfDeduction = salary.getPfEmployeeContribution().multiply(attendanceRatio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal esiDeduction = salary.getEsiEmployeeContribution(grossEarnings).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ptDeduction = salary.getProfessionalTaxAmount();

        // Calculate LOP (Loss of Pay)
        BigDecimal lopDays = BigDecimal.valueOf(totalWorkingDays).subtract(effectiveWorkingDays);
        BigDecimal dailyGross = grossEarnings.divide(BigDecimal.valueOf(totalWorkingDays), 2, RoundingMode.HALF_UP);
        BigDecimal lopDeduction = lopDays.multiply(dailyGross).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalDeductions = pfDeduction.add(esiDeduction).add(ptDeduction).add(lopDeduction);

        // Calculate overtime pay
        BigDecimal hourlyRate = salary.getBasicSalary().divide(BigDecimal.valueOf(208), 2, RoundingMode.HALF_UP); // 208 hours/month
        BigDecimal overtimePay = overtimeHours.multiply(hourlyRate).multiply(new BigDecimal("2")).setScale(2, RoundingMode.HALF_UP); // 2x for overtime

        // Calculate net pay
        BigDecimal netPay = grossEarnings.subtract(totalDeductions).add(overtimePay);

        return Payroll.builder()
                .employee(employee)
                .month(month)
                .year(year)
                .totalWorkingDays(totalWorkingDays)
                .presentDays(presentDays)
                .absentDays(BigDecimal.valueOf(totalWorkingDays).subtract(effectiveWorkingDays))
                .leaveDays(BigDecimal.ZERO) // TODO: Calculate from leave data
                .halfDays(halfDays)
                .weeklyOffDays(weeklyOffDays)
                .holidays(0)
                .basicEarned(basicEarned)
                .hraEarned(hraEarned)
                .daEarned(daEarned)
                .conveyanceEarned(conveyanceEarned)
                .medicalEarned(medicalEarned)
                .specialEarned(specialEarned)
                .totalAllowances(daEarned.add(conveyanceEarned).add(medicalEarned).add(specialEarned))
                .grossEarnings(grossEarnings)
                .pfDeduction(pfDeduction)
                .esiDeduction(esiDeduction)
                .ptDeduction(ptDeduction)
                .lopDeduction(lopDeduction)
                .totalDeductions(totalDeductions)
                .netPay(netPay)
                .overtimeHours(overtimeHours)
                .overtimePay(overtimePay)
                .status(PayrollStatus.DRAFT)
                .build();
    }

    private int calculateWorkingDays(int month, int year) {
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.with(TemporalAdjusters.lastDayOfMonth());

        int workingDays = 0;
        LocalDate date = startOfMonth;
        while (!date.isAfter(endOfMonth)) {
            if (date.getDayOfWeek().getValue() <= 5) { // Monday to Friday
                workingDays++;
            }
            date = date.plusDays(1);
        }
        return workingDays;
    }

    private Pageable createPageable(PaginationRequest request) {
        Sort sort = Sort.by(request.getSortDirection().equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC,
                request.getSortBy());
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }
}
