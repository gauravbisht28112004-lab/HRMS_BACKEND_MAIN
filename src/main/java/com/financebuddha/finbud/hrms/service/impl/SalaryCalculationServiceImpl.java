package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.enums.SalaryStructureType;
import com.financebuddha.finbud.hrms.exception.BadRequestException;
import com.financebuddha.finbud.hrms.service.SalaryCalculationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure-function implementation of {@link SalaryCalculationService}. No
 * Spring-managed state beyond being a singleton bean — safe to unit-test
 * with {@code new SalaryCalculationServiceImpl()}.
 */
@Slf4j
@Service
public class SalaryCalculationServiceImpl implements SalaryCalculationService {

    @Override
    public CtcCalculationOutput calculate(CtcCalculationInput in) {
        validate(in);

        Policy policy = in.getPolicy();
        int p = policy.getPrecisionScale();
        int o = policy.getOutputScale();

        BigDecimal monthlyGross = nz(in.getMonthlyGrossCtc());
        BigDecimal workingDaysBd = BigDecimal.valueOf(in.getWorkingDays());
        BigDecimal lopDays = nz(in.getLopDays());

        // ------------------------------------------------------------------
        // LOP pro-ration: gross = monthlyGrossCtc * (workingDays - lopDays) / workingDays
        // ------------------------------------------------------------------
        BigDecimal effectiveDays = workingDaysBd.subtract(lopDays);
        BigDecimal grossEarnings = monthlyGross
                .multiply(effectiveDays)
                .divide(workingDaysBd, p, RoundingMode.HALF_UP);
        BigDecimal lopDeduction = monthlyGross.subtract(grossEarnings).setScale(p, RoundingMode.HALF_UP);

        // ------------------------------------------------------------------
        // Per-structure deductions
        // ------------------------------------------------------------------
        BigDecimal employerPf  = BigDecimal.ZERO;
        BigDecimal employeePf  = BigDecimal.ZERO;
        BigDecimal employerEsi = BigDecimal.ZERO;
        BigDecimal employeeEsi = BigDecimal.ZERO;
        BigDecimal lwf         = BigDecimal.ZERO;
        BigDecimal tds         = BigDecimal.ZERO;

        SalaryStructureType type = in.getStructureType();
        if (type == SalaryStructureType.CONTRACT) {
            // No PF, no ESI, no LWF. TDS = gross * rate%, override takes precedence.
            if (in.getTdsOverride() != null) {
                tds = in.getTdsOverride();
            } else {
                BigDecimal rate = in.getTdsRatePercentOverride() != null
                        ? in.getTdsRatePercentOverride()
                        : nz(policy.getContractTdsRatePercent());
                tds = grossEarnings.multiply(rate).divide(BigDecimal.valueOf(100), p, RoundingMode.HALF_UP);
            }
        } else if (type == SalaryStructureType.MANAGEMENT || type == SalaryStructureType.HIGHLY_SKILLED) {
            employerPf  = firstNonNull(in.getEmployerPfOverride(),  policy.getDefaultEmployerPf());
            employeePf  = firstNonNull(in.getEmployeePfOverride(),  policy.getDefaultEmployeePf());
            employerEsi = nz(in.getEmployerEsiOverride());
            employeeEsi = nz(in.getEmployeeEsiOverride());
            lwf         = firstNonNull(in.getLwfOverride(),         policy.getDefaultLwf());
            tds         = nz(in.getTdsOverride());
        }

        // Note: pro-rate PF/ESI if the employee had LOP days. Keeping non-prorated
        // to match the Finbud master (ND33004 comparison — HR reported PF = 1950 flat).
        // If policy changes, multiply each by (effectiveDays / workingDays) here.

        BigDecimal incentives   = nz(in.getIncentives());
        BigDecimal adjustments  = nz(in.getAdjustments());

        // ------------------------------------------------------------------
        // Totals
        // ------------------------------------------------------------------
        // Convention: totalDeductions aggregates the employee-side outflows and
        // LWF/TDS. Employer contributions are tracked separately on the payroll
        // row but NOT subtracted from NTH (they're CTC-included already).
        BigDecimal totalDeductions = employeePf
                .add(employeeEsi)
                .add(lwf)
                .add(tds)
                .setScale(p, RoundingMode.HALF_UP);

        BigDecimal netPay = grossEarnings
                .subtract(totalDeductions)
                .add(incentives)
                .add(adjustments)
                .setScale(p, RoundingMode.HALF_UP);

        if (log.isDebugEnabled()) {
            log.debug("calc[{}]: gross={} emplPf={} emplEsi={} lwf={} tds={} inc={} adj={} -> net={}",
                    type, grossEarnings, employeePf, employeeEsi, lwf, tds, incentives, adjustments, netPay);
        }

        // ------------------------------------------------------------------
        // Scale everything to output precision before returning
        // ------------------------------------------------------------------
        return CtcCalculationOutput.builder()
                .monthlyGrossCtc(scale(monthlyGross, o))
                .grossEarnings(scale(grossEarnings, o))
                .lopDeduction(scale(lopDeduction, o))
                .employerPf(scale(employerPf, o))
                .employeePf(scale(employeePf, o))
                .employerEsi(scale(employerEsi, o))
                .employeeEsi(scale(employeeEsi, o))
                .lwfAmount(scale(lwf, o))
                .tdsAmount(scale(tds, o))
                .totalDeductions(scale(totalDeductions, o))
                .incentives(scale(incentives, o))
                .adjustments(scale(adjustments, o))
                .adjustmentReason(in.getAdjustmentReason())
                .netPay(scale(netPay, o))
                .build();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void validate(CtcCalculationInput in) {
        if (in == null) throw new BadRequestException("CtcCalculationInput is required");
        if (in.getStructureType() == null) throw new BadRequestException("structureType is required");
        if (in.getMonthlyGrossCtc() == null || in.getMonthlyGrossCtc().signum() < 0) {
            throw new BadRequestException("monthlyGrossCtc must be non-negative");
        }
        if (in.getWorkingDays() == null || in.getWorkingDays() <= 0) {
            throw new BadRequestException("workingDays must be > 0");
        }
        if (in.getLopDays() != null && in.getLopDays().signum() < 0) {
            throw new BadRequestException("lopDays must be non-negative");
        }
        if (in.getPolicy() == null) throw new BadRequestException("policy is required");
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal firstNonNull(BigDecimal a, BigDecimal b) {
        if (a != null) return a;
        if (b != null) return b;
        return BigDecimal.ZERO;
    }

    private static BigDecimal scale(BigDecimal v, int scale) {
        return v == null ? null : v.setScale(scale, RoundingMode.HALF_UP);
    }
}
