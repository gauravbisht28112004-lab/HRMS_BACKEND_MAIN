package com.financebuddha.finbud.hrms.service;

import com.financebuddha.finbud.hrms.enums.SalaryStructureType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Pure-function CTC → NTH (Net Take Home) calculator.
 * <p>
 * This is the single source of truth for Finbud payroll math. It is intentionally
 * side-effect free and testable in isolation — no repository access, no DB writes.
 * Policy constants (default PF, TDS rate, precision) are read from
 * {@link SystemConfigService} and passed in as {@link CtcCalculationInput#getPolicy()}
 * so the callers control both sourcing and caching.
 * <p>
 * Formulas (Finbud policy, 2026):
 * <ul>
 *   <li><b>CONTRACT</b>: NTH = Gross - TDS, where TDS = Gross * 5% (no PF, no ESI, no LWF)</li>
 *   <li><b>MANAGEMENT</b>: NTH = Gross - EmployerPF - EmployeePF - LWF - TDS + adjustments
 *       (PF is a fixed INR 1950 on both sides, not a percentage)</li>
 *   <li><b>HIGHLY_SKILLED</b>: same shape as MANAGEMENT</li>
 * </ul>
 * LOP pro-ration applies on top: Gross = monthlyGrossCtc * (workingDays - lopDays) / workingDays.
 */
public interface SalaryCalculationService {

    /**
     * Compute a full payroll line given the CTC structure and the attendance
     * context for the month. The returned {@link CtcCalculationOutput} is
     * ready to be copied onto a {@code Payroll} entity.
     */
    CtcCalculationOutput calculate(CtcCalculationInput input);

    // ------------------------------------------------------------------
    // Nested types — live here to keep the service interface self-contained
    // ------------------------------------------------------------------

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class CtcCalculationInput {

        /** Which Finbud salary structure this employee is on. Required. */
        private SalaryStructureType structureType;

        /** Monthly gross CTC (full-month amount, pre-pro-ration). Required. */
        private BigDecimal monthlyGrossCtc;

        /** Working days for the month (e.g. 30, 31). Required. */
        private Integer workingDays;

        /** Loss-of-pay days. Null is treated as zero. */
        private BigDecimal lopDays;

        /**
         * Optional overrides from the stored SalaryStructure — when null, the
         * calculator falls back to {@link Policy} values. Callers should pass
         * explicit overrides only when an employee has a negotiated exception
         * (ND33177 / ND33301 documented cases).
         */
        private BigDecimal employerPfOverride;
        private BigDecimal employeePfOverride;
        private BigDecimal employerEsiOverride;
        private BigDecimal employeeEsiOverride;
        private BigDecimal lwfOverride;
        private BigDecimal tdsOverride;
        private BigDecimal tdsRatePercentOverride;

        /** Reconciliation adjustment (e.g. +150 to close the ND33004 rounding gap). Null = zero. */
        private BigDecimal adjustments;
        private String adjustmentReason;

        /** Incentives credited this month. Null = zero. */
        private BigDecimal incentives;

        /** Policy constants — required. Caller should source these via SystemConfigService. */
        private Policy policy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class CtcCalculationOutput {

        // Gross / pro-ration
        private BigDecimal monthlyGrossCtc;        // input echo, at output scale
        private BigDecimal grossEarnings;          // after LOP pro-ration
        private BigDecimal lopDeduction;           // monthlyGrossCtc - grossEarnings

        // Deductions (all filled; zero when structure type doesn't apply)
        private BigDecimal employerPf;
        private BigDecimal employeePf;
        private BigDecimal employerEsi;
        private BigDecimal employeeEsi;
        private BigDecimal lwfAmount;
        private BigDecimal tdsAmount;
        private BigDecimal totalDeductions;

        // Incentives / adjustments
        private BigDecimal incentives;
        private BigDecimal adjustments;
        private String adjustmentReason;

        // Final
        private BigDecimal netPay;
    }

    /** Policy constants shared across all calculations for a given month. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class Policy {
        /** Fixed employer PF amount for MANAGEMENT / HIGHLY_SKILLED (INR). Finbud default: 1950. */
        private BigDecimal defaultEmployerPf;

        /** Fixed employee PF amount for MANAGEMENT / HIGHLY_SKILLED (INR). Finbud default: 1950. */
        private BigDecimal defaultEmployeePf;

        /** Default LWF deduction (INR). Finbud default: 0. */
        private BigDecimal defaultLwf;

        /** TDS rate applied to CONTRACT gross (percent). Finbud default: 5.00. */
        private BigDecimal contractTdsRatePercent;

        /** BigDecimal scale for intermediate math (typically 4). */
        private int precisionScale;

        /** BigDecimal scale for persisted / output values (typically 2). */
        private int outputScale;
    }
}
