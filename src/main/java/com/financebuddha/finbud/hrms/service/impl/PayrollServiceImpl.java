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
import com.financebuddha.finbud.hrms.security.AuthzService;
import com.financebuddha.finbud.hrms.service.PayrollService;
import com.financebuddha.finbud.hrms.service.SalaryCalculationService;
import com.financebuddha.finbud.hrms.service.SalaryCalculationService.CtcCalculationInput;
import com.financebuddha.finbud.hrms.service.SalaryCalculationService.CtcCalculationOutput;
import com.financebuddha.finbud.hrms.service.SalaryCalculationService.Policy;
import com.financebuddha.finbud.hrms.service.SystemConfigService;
import com.financebuddha.finbud.hrms.service.payroll.PayslipPdfGenerator;
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
    private final SalaryCalculationService salaryCalculationService;
    private final SystemConfigService systemConfig;
    private final PayslipPdfGenerator payslipPdfGenerator;
    private final AuthzService authz;

    // Default policy fallbacks when SystemConfig keys are missing. These
    // match the Flyway V5 seed values — update both in lockstep.
    private static final BigDecimal FB_DEFAULT_EMPLOYER_PF = new BigDecimal("1950.00");
    private static final BigDecimal FB_DEFAULT_EMPLOYEE_PF = new BigDecimal("1950.00");
    private static final BigDecimal FB_DEFAULT_LWF         = BigDecimal.ZERO;
    private static final BigDecimal FB_CONTRACT_TDS_PCT    = new BigDecimal("5.00");
    private static final int FB_DEFAULT_PRECISION_SCALE    = 4;
    private static final int FB_DEFAULT_OUTPUT_SCALE       = 2;

    @Override
    @Transactional
    public PayrollResponse generatePayroll(Long employeeId, Integer month, Integer year) {
        PayrollGenerateRequest req = new PayrollGenerateRequest();
        req.setEmployeeId(employeeId);
        req.setMonth(month);
        req.setYear(year);
        return generatePayroll(req);
    }

    @Override
    @Transactional
    public PayrollResponse generatePayroll(PayrollGenerateRequest request) {
        Long employeeId = request.getEmployeeId();
        Integer month = request.getMonth();
        Integer year = request.getYear();
        if (employeeId == null) {
            throw new BadRequestException("employeeId is required when generating a single payroll");
        }
        log.info("Generating payroll for employee {} for {}/{} (manualLop={}, incentivesOverride={}, adjustments={})",
                employeeId, month, year, request.getLopDays(), request.getIncentivesOverride(), request.getAdjustments());

        if (payrollRepository.existsByEmployeeIdAndMonthAndYear(employeeId, month, year)) {
            throw new BadRequestException("Payroll already exists for this employee for " + month + "/" + year);
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", employeeId));

        // Resolve the salary structure effective for this pay month — falls
        // back to "active" for backward compatibility with pre-V4 records
        // that never set effective_from.
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.with(TemporalAdjusters.lastDayOfMonth());

        SalaryStructure salary = salaryStructureRepository
                .findEffectiveForEmployee(employeeId, endOfMonth)
                .or(() -> salaryStructureRepository.findByEmployeeIdAndIsActiveTrue(employeeId))
                .orElseThrow(() -> new ResourceNotFoundException("SalaryStructure", "employeeId", employeeId));

        List<Attendance> attendances = attendanceRepository
                .findByEmployeeIdAndAttendanceDateBetween(employeeId, startOfMonth, endOfMonth);

        Payroll payroll;
        if (salary.getStructureType() != null && salary.getMonthlyGrossCtc() != null) {
            payroll = calculateCtcPayroll(employee, salary, attendances, month, year, request);
        } else {
            // Legacy component-based calc — preserved so historical payrolls
            // generated before the CTC model migrated can still be reproduced.
            payroll = calculateLegacyPayroll(employee, salary, attendances, month, year);
        }
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

        // Bulk flow intentionally does NOT propagate per-run manual
        // overrides (lopDays / incentives / adjustments) — those only make
        // sense for a single employee run. Callers who need overrides must
        // use the single-employee flow.
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
        authz.requireOwnerOrPrivileged(employeeId);
        Pageable pageable = createPageable(paginationRequest);
        Page<Payroll> payrollPage = payrollRepository.findByEmployeeId(employeeId, pageable);

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
    @Transactional
    public byte[] generatePayslipPdf(Long payrollId) {
        log.info("Generating payslip PDF for payroll: {}", payrollId);
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll", "id", payrollId));
        byte[] pdf = payslipPdfGenerator.generate(payroll);
        // Flag the payroll so the UI can show "payslip available" without
        // having to call the PDF endpoint just to check.
        if (!Boolean.TRUE.equals(payroll.getPayslipGenerated())) {
            payroll.setPayslipGenerated(true);
            payrollRepository.save(payroll);
        }
        return pdf;
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

    // ------------------------------------------------------------------
    // CTC-model calculation — delegates math to SalaryCalculationService
    // ------------------------------------------------------------------

    private Payroll calculateCtcPayroll(Employee employee, SalaryStructure salary, List<Attendance> attendances,
                                        Integer month, Integer year, PayrollGenerateRequest overrides) {

        int totalWorkingDays = calculateWorkingDays(month, year);

        // When HR submits a manual LOP for this run we honor it verbatim —
        // the Finbud roster is master-sheet driven and attendance is still
        // being on-boarded, so "trust the attendance table" is not yet a
        // safe default. Fall back to the attendance computation only when
        // the caller left lopDays null.
        BigDecimal lopDays = overrides != null && overrides.getLopDays() != null
                ? overrides.getLopDays()
                : computeLopDays(attendances, totalWorkingDays);
        if (lopDays.signum() < 0) {
            lopDays = BigDecimal.ZERO;
        }
        BigDecimal maxLop = BigDecimal.valueOf(totalWorkingDays);
        if (lopDays.compareTo(maxLop) > 0) {
            lopDays = maxLop;
        }

        Policy policy = Policy.builder()
                .defaultEmployerPf(systemConfig.getBigDecimal(
                        SystemConfigService.Keys.PAYROLL_PF_EMPLOYER_DEFAULT, FB_DEFAULT_EMPLOYER_PF))
                .defaultEmployeePf(systemConfig.getBigDecimal(
                        SystemConfigService.Keys.PAYROLL_PF_EMPLOYEE_DEFAULT, FB_DEFAULT_EMPLOYEE_PF))
                .defaultLwf(systemConfig.getBigDecimal(
                        SystemConfigService.Keys.PAYROLL_LWF_DEFAULT, FB_DEFAULT_LWF))
                .contractTdsRatePercent(systemConfig.getBigDecimal(
                        SystemConfigService.Keys.PAYROLL_TDS_CONTRACT_RATE_PCT, FB_CONTRACT_TDS_PCT))
                .precisionScale(systemConfig.getInt(
                        SystemConfigService.Keys.PAYROLL_CALC_PRECISION_SCALE, FB_DEFAULT_PRECISION_SCALE))
                .outputScale(systemConfig.getInt(
                        SystemConfigService.Keys.PAYROLL_CALC_OUTPUT_SCALE, FB_DEFAULT_OUTPUT_SCALE))
                .build();

        // Per-run incentives override wins over the standing structure value.
        BigDecimal incentivesForRun = overrides != null && overrides.getIncentivesOverride() != null
                ? overrides.getIncentivesOverride()
                : salary.getIncentives();

        CtcCalculationInput input = CtcCalculationInput.builder()
                .structureType(salary.getStructureType())
                .monthlyGrossCtc(salary.getMonthlyGrossCtc())
                .workingDays(totalWorkingDays)
                .lopDays(lopDays)
                .employerPfOverride(salary.getEmployerPf())
                .employeePfOverride(salary.getEmployeePf())
                .employerEsiOverride(salary.getEmployerEsi())
                .employeeEsiOverride(salary.getEmployeeEsi())
                .lwfOverride(salary.getLwfAmount())
                .tdsOverride(salary.getTdsAmount())
                .tdsRatePercentOverride(salary.getTdsRatePercent())
                .incentives(incentivesForRun)
                .policy(policy)
                .build();

        CtcCalculationOutput out = salaryCalculationService.calculate(input);

        // Reconciliation adjustments — free-form positive or negative delta
        // applied on top of the calculated net pay, with an auditable reason.
        BigDecimal adjustmentsForRun = overrides != null && overrides.getAdjustments() != null
                ? overrides.getAdjustments()
                : (out.getAdjustments() != null ? out.getAdjustments() : BigDecimal.ZERO);
        String adjustmentReasonForRun = overrides != null && overrides.getAdjustmentReason() != null
                ? overrides.getAdjustmentReason()
                : out.getAdjustmentReason();

        BigDecimal baseNet = out.getNetPay() != null ? out.getNetPay() : BigDecimal.ZERO;
        BigDecimal adjustedNet = baseNet;
        if (overrides != null && overrides.getAdjustments() != null) {
            adjustedNet = baseNet.add(overrides.getAdjustments());
        }

        BigDecimal presentDays = BigDecimal.valueOf(totalWorkingDays).subtract(lopDays);

        return Payroll.builder()
                .employee(employee)
                .month(month)
                .year(year)
                .totalWorkingDays(totalWorkingDays)
                .workingDays(totalWorkingDays)
                .presentDays(presentDays)
                .absentDays(lopDays)
                .leaveDays(BigDecimal.ZERO)
                .halfDays(BigDecimal.ZERO)
                .weeklyOffDays(0)
                .holidays(0)
                .lopDays(lopDays)
                // CTC fields
                .structureType(salary.getStructureType())
                .monthlyGrossCtc(out.getMonthlyGrossCtc())
                .grossEarnings(out.getGrossEarnings())
                .employerPf(out.getEmployerPf())
                .employeePf(out.getEmployeePf())
                .employerEsi(out.getEmployerEsi())
                .employeeEsi(out.getEmployeeEsi())
                .lwfAmount(out.getLwfAmount())
                .tdsAmount(out.getTdsAmount())
                .lopDeduction(out.getLopDeduction())
                .totalDeductions(out.getTotalDeductions())
                .incentiveAmount(out.getIncentives())
                .adjustments(adjustmentsForRun != null ? adjustmentsForRun : BigDecimal.ZERO)
                .adjustmentReason(adjustmentReasonForRun)
                .netPay(adjustedNet)
                // Legacy PT: not part of the CTC model
                .ptDeduction(BigDecimal.ZERO)
                .status(PayrollStatus.DRAFT)
                .build();
    }

    private BigDecimal computeLopDays(List<Attendance> attendances, int totalWorkingDays) {
        BigDecimal presentDays = BigDecimal.ZERO;
        BigDecimal halfDays = BigDecimal.ZERO;
        for (Attendance a : attendances) {
            if (a.getStatus() == AttendanceStatus.PRESENT) {
                if (Boolean.TRUE.equals(a.getIsHalfDay())) {
                    halfDays = halfDays.add(new BigDecimal("0.5"));
                    presentDays = presentDays.add(new BigDecimal("0.5"));
                } else {
                    presentDays = presentDays.add(BigDecimal.ONE);
                }
            }
        }
        BigDecimal effectiveDays = presentDays.add(halfDays.multiply(new BigDecimal("0.5")));
        BigDecimal lop = BigDecimal.valueOf(totalWorkingDays).subtract(effectiveDays);
        if (lop.signum() < 0) lop = BigDecimal.ZERO;
        return lop;
    }

    // ------------------------------------------------------------------
    // Legacy component-based calculation — kept for backward compatibility
    // ------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    private Payroll calculateLegacyPayroll(Employee employee, SalaryStructure salary, List<Attendance> attendances,
                                           Integer month, Integer year) {

        int totalWorkingDays = calculateWorkingDays(month, year);

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

        BigDecimal effectiveWorkingDays = presentDays.add(halfDays.multiply(new BigDecimal("0.5")));
        BigDecimal attendanceRatio = totalWorkingDays > 0 ?
                effectiveWorkingDays.divide(BigDecimal.valueOf(totalWorkingDays), 4, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        BigDecimal basic = zeroIfNull(salary.getBasicSalary());
        BigDecimal hra = zeroIfNull(salary.getHra());
        BigDecimal da = zeroIfNull(salary.getDa());
        BigDecimal conveyance = zeroIfNull(salary.getConveyanceAllowance());
        BigDecimal medical = zeroIfNull(salary.getMedicalAllowance());
        BigDecimal special = zeroIfNull(salary.getSpecialAllowance());

        BigDecimal basicEarned      = basic.multiply(attendanceRatio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal hraEarned        = hra.multiply(attendanceRatio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal daEarned         = da.multiply(attendanceRatio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal conveyanceEarned = conveyance.multiply(attendanceRatio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal medicalEarned    = medical.multiply(attendanceRatio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal specialEarned    = special.multiply(attendanceRatio).setScale(2, RoundingMode.HALF_UP);

        BigDecimal grossEarnings = basicEarned.add(hraEarned).add(daEarned)
                .add(conveyanceEarned).add(medicalEarned).add(specialEarned);

        BigDecimal pfDeduction  = salary.getPfEmployeeContribution().multiply(attendanceRatio).setScale(2, RoundingMode.HALF_UP);
        BigDecimal esiDeduction = salary.getEsiEmployeeContribution(grossEarnings).setScale(2, RoundingMode.HALF_UP);
        BigDecimal ptDeduction  = zeroIfNull(salary.getProfessionalTaxAmount());

        BigDecimal lopDays = BigDecimal.valueOf(totalWorkingDays).subtract(effectiveWorkingDays);
        BigDecimal dailyGross = totalWorkingDays > 0
                ? grossEarnings.divide(BigDecimal.valueOf(totalWorkingDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal lopDeduction = lopDays.multiply(dailyGross).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalDeductions = pfDeduction.add(esiDeduction).add(ptDeduction).add(lopDeduction);

        BigDecimal hourlyRate = basic.signum() > 0
                ? basic.divide(BigDecimal.valueOf(208), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal overtimePay = overtimeHours.multiply(hourlyRate).multiply(new BigDecimal("2")).setScale(2, RoundingMode.HALF_UP);

        BigDecimal netPay = grossEarnings.subtract(totalDeductions).add(overtimePay);

        return Payroll.builder()
                .employee(employee)
                .month(month)
                .year(year)
                .totalWorkingDays(totalWorkingDays)
                .presentDays(presentDays)
                .absentDays(BigDecimal.valueOf(totalWorkingDays).subtract(effectiveWorkingDays))
                .leaveDays(BigDecimal.ZERO)
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

    private static BigDecimal zeroIfNull(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
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
