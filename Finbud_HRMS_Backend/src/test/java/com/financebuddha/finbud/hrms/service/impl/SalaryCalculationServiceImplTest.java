package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.enums.SalaryStructureType;
import com.financebuddha.finbud.hrms.exception.BadRequestException;
import com.financebuddha.finbud.hrms.service.SalaryCalculationService.CtcCalculationInput;
import com.financebuddha.finbud.hrms.service.SalaryCalculationService.CtcCalculationOutput;
import com.financebuddha.finbud.hrms.service.SalaryCalculationService.Policy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SalaryCalculationServiceImpl}. This is a pure-function
 * calculator, so no mocks are needed — we construct inputs and assert exact
 * BigDecimal output (at the Finbud policy precision scale of 2).
 *
 * <p>Golden-number scenarios mirror the Finbud master data:
 * <ul>
 *   <li>CONTRACT: TDS = Gross × 5% (no PF, no ESI, no LWF)</li>
 *   <li>MANAGEMENT / HIGHLY_SKILLED: PF fixed at ₹1,950 each side, LWF/TDS via policy</li>
 *   <li>LOP pro-ration: Gross = monthlyGrossCtc × (workingDays − lopDays) / workingDays</li>
 * </ul>
 */
class SalaryCalculationServiceImplTest {

    private final SalaryCalculationServiceImpl service = new SalaryCalculationServiceImpl();

    /** Finbud-default policy (matches Flyway V5 seed data). */
    private Policy defaultPolicy() {
        return Policy.builder()
                .defaultEmployerPf(new BigDecimal("1950"))
                .defaultEmployeePf(new BigDecimal("1950"))
                .defaultLwf(BigDecimal.ZERO)
                .contractTdsRatePercent(new BigDecimal("5.00"))
                .precisionScale(4)
                .outputScale(2)
                .build();
    }

    // ------------------------------------------------------------------
    // CONTRACT
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("CONTRACT structure")
    class Contract {

        @Test
        @DisplayName("Full-month contract: NTH = Gross × 95%, no PF/ESI/LWF")
        void contractFullMonth() {
            CtcCalculationInput in = CtcCalculationInput.builder()
                    .structureType(SalaryStructureType.CONTRACT)
                    .monthlyGrossCtc(new BigDecimal("50000"))
                    .workingDays(30)
                    .policy(defaultPolicy())
                    .build();

            CtcCalculationOutput out = service.calculate(in);

            assertThat(out.getGrossEarnings()).isEqualByComparingTo("50000.00");
            assertThat(out.getEmployerPf()).isEqualByComparingTo("0.00");
            assertThat(out.getEmployeePf()).isEqualByComparingTo("0.00");
            assertThat(out.getEmployerEsi()).isEqualByComparingTo("0.00");
            assertThat(out.getEmployeeEsi()).isEqualByComparingTo("0.00");
            assertThat(out.getLwfAmount()).isEqualByComparingTo("0.00");
            assertThat(out.getTdsAmount()).isEqualByComparingTo("2500.00");
            assertThat(out.getTotalDeductions()).isEqualByComparingTo("2500.00");
            assertThat(out.getNetPay()).isEqualByComparingTo("47500.00");
            assertThat(out.getLopDeduction()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("Contract TDS rate override takes precedence over policy default")
        void contractRateOverride() {
            CtcCalculationInput in = CtcCalculationInput.builder()
                    .structureType(SalaryStructureType.CONTRACT)
                    .monthlyGrossCtc(new BigDecimal("40000"))
                    .workingDays(30)
                    .tdsRatePercentOverride(new BigDecimal("10.00"))
                    .policy(defaultPolicy())
                    .build();

            CtcCalculationOutput out = service.calculate(in);

            // 40 000 × 10% = 4 000
            assertThat(out.getTdsAmount()).isEqualByComparingTo("4000.00");
            assertThat(out.getNetPay()).isEqualByComparingTo("36000.00");
        }

        @Test
        @DisplayName("Explicit TDS override beats both rate-override and policy")
        void contractTdsAbsoluteOverride() {
            CtcCalculationInput in = CtcCalculationInput.builder()
                    .structureType(SalaryStructureType.CONTRACT)
                    .monthlyGrossCtc(new BigDecimal("40000"))
                    .workingDays(30)
                    .tdsOverride(new BigDecimal("1234.56"))
                    .tdsRatePercentOverride(new BigDecimal("99.00")) // should be ignored
                    .policy(defaultPolicy())
                    .build();

            CtcCalculationOutput out = service.calculate(in);
            assertThat(out.getTdsAmount()).isEqualByComparingTo("1234.56");
            assertThat(out.getNetPay()).isEqualByComparingTo("38765.44");
        }
    }

    // ------------------------------------------------------------------
    // MANAGEMENT / HIGHLY_SKILLED
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("MANAGEMENT / HIGHLY_SKILLED structure")
    class ManagementAndHighlySkilled {

        @Test
        @DisplayName("MANAGEMENT full-month: PF 1950 each side, no LWF/TDS, NTH = Gross − employeePf")
        void managementFullMonth() {
            CtcCalculationInput in = CtcCalculationInput.builder()
                    .structureType(SalaryStructureType.MANAGEMENT)
                    .monthlyGrossCtc(new BigDecimal("100000"))
                    .workingDays(30)
                    .policy(defaultPolicy())
                    .build();

            CtcCalculationOutput out = service.calculate(in);

            assertThat(out.getGrossEarnings()).isEqualByComparingTo("100000.00");
            assertThat(out.getEmployerPf()).isEqualByComparingTo("1950.00");
            assertThat(out.getEmployeePf()).isEqualByComparingTo("1950.00");
            assertThat(out.getLwfAmount()).isEqualByComparingTo("0.00");
            assertThat(out.getTdsAmount()).isEqualByComparingTo("0.00");
            // Employer PF is NOT subtracted from NTH — only employee-side + LWF + TDS.
            assertThat(out.getTotalDeductions()).isEqualByComparingTo("1950.00");
            assertThat(out.getNetPay()).isEqualByComparingTo("98050.00");
        }

        @Test
        @DisplayName("HIGHLY_SKILLED mirrors MANAGEMENT shape")
        void highlySkilledMatchesManagement() {
            CtcCalculationInput base = CtcCalculationInput.builder()
                    .monthlyGrossCtc(new BigDecimal("80000"))
                    .workingDays(30)
                    .policy(defaultPolicy())
                    .build();

            CtcCalculationOutput mgmt = service.calculate(CtcCalculationInput.builder()
                    .structureType(SalaryStructureType.MANAGEMENT)
                    .monthlyGrossCtc(base.getMonthlyGrossCtc())
                    .workingDays(base.getWorkingDays())
                    .policy(base.getPolicy())
                    .build());

            CtcCalculationOutput hs = service.calculate(CtcCalculationInput.builder()
                    .structureType(SalaryStructureType.HIGHLY_SKILLED)
                    .monthlyGrossCtc(base.getMonthlyGrossCtc())
                    .workingDays(base.getWorkingDays())
                    .policy(base.getPolicy())
                    .build());

            assertThat(hs.getNetPay()).isEqualByComparingTo(mgmt.getNetPay());
            assertThat(hs.getEmployerPf()).isEqualByComparingTo(mgmt.getEmployerPf());
            assertThat(hs.getEmployeePf()).isEqualByComparingTo(mgmt.getEmployeePf());
            assertThat(hs.getTotalDeductions()).isEqualByComparingTo(mgmt.getTotalDeductions());
        }

        @Test
        @DisplayName("PF overrides trump policy defaults (ND33177-style negotiated exception)")
        void pfOverridesApplied() {
            CtcCalculationInput in = CtcCalculationInput.builder()
                    .structureType(SalaryStructureType.MANAGEMENT)
                    .monthlyGrossCtc(new BigDecimal("90000"))
                    .workingDays(30)
                    .employerPfOverride(new BigDecimal("0"))
                    .employeePfOverride(new BigDecimal("0"))
                    .policy(defaultPolicy())
                    .build();

            CtcCalculationOutput out = service.calculate(in);
            assertThat(out.getEmployerPf()).isEqualByComparingTo("0.00");
            assertThat(out.getEmployeePf()).isEqualByComparingTo("0.00");
            assertThat(out.getNetPay()).isEqualByComparingTo("90000.00");
        }

        @Test
        @DisplayName("Adjustments and incentives flow through to net pay")
        void incentivesAndAdjustments() {
            CtcCalculationInput in = CtcCalculationInput.builder()
                    .structureType(SalaryStructureType.MANAGEMENT)
                    .monthlyGrossCtc(new BigDecimal("100000"))
                    .workingDays(30)
                    .incentives(new BigDecimal("5000"))
                    .adjustments(new BigDecimal("150"))
                    .adjustmentReason("ND33004 rounding reconciliation")
                    .policy(defaultPolicy())
                    .build();

            CtcCalculationOutput out = service.calculate(in);

            // 100 000 − 1 950 (employee PF) + 5 000 + 150 = 103 200
            assertThat(out.getNetPay()).isEqualByComparingTo("103200.00");
            assertThat(out.getAdjustments()).isEqualByComparingTo("150.00");
            assertThat(out.getIncentives()).isEqualByComparingTo("5000.00");
            assertThat(out.getAdjustmentReason()).isEqualTo("ND33004 rounding reconciliation");
        }
    }

    // ------------------------------------------------------------------
    // LOP pro-ration
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("LOP pro-ration")
    class LossOfPay {

        @Test
        @DisplayName("MANAGEMENT with 3 LOP days on a 30-day month: gross scaled to 27/30 of CTC")
        void managementWithLop() {
            CtcCalculationInput in = CtcCalculationInput.builder()
                    .structureType(SalaryStructureType.MANAGEMENT)
                    .monthlyGrossCtc(new BigDecimal("60000"))
                    .workingDays(30)
                    .lopDays(new BigDecimal("3"))
                    .policy(defaultPolicy())
                    .build();

            CtcCalculationOutput out = service.calculate(in);

            // 60 000 × 27/30 = 54 000
            assertThat(out.getGrossEarnings()).isEqualByComparingTo("54000.00");
            assertThat(out.getLopDeduction()).isEqualByComparingTo("6000.00");
            // PF still fixed at 1 950 (no pro-ration — matches ND33004 reference)
            assertThat(out.getEmployeePf()).isEqualByComparingTo("1950.00");
            assertThat(out.getNetPay()).isEqualByComparingTo("52050.00");
        }

        @Test
        @DisplayName("CONTRACT with LOP: TDS recomputed on the pro-rated gross")
        void contractWithLop() {
            CtcCalculationInput in = CtcCalculationInput.builder()
                    .structureType(SalaryStructureType.CONTRACT)
                    .monthlyGrossCtc(new BigDecimal("30000"))
                    .workingDays(30)
                    .lopDays(new BigDecimal("6"))
                    .policy(defaultPolicy())
                    .build();

            CtcCalculationOutput out = service.calculate(in);

            // Gross = 30 000 × 24/30 = 24 000 ; TDS = 24 000 × 5% = 1 200
            assertThat(out.getGrossEarnings()).isEqualByComparingTo("24000.00");
            assertThat(out.getTdsAmount()).isEqualByComparingTo("1200.00");
            assertThat(out.getNetPay()).isEqualByComparingTo("22800.00");
            assertThat(out.getLopDeduction()).isEqualByComparingTo("6000.00");
        }

        @Test
        @DisplayName("Fractional LOP days are honoured")
        void fractionalLopDays() {
            CtcCalculationInput in = CtcCalculationInput.builder()
                    .structureType(SalaryStructureType.CONTRACT)
                    .monthlyGrossCtc(new BigDecimal("30000"))
                    .workingDays(30)
                    .lopDays(new BigDecimal("0.5"))
                    .policy(defaultPolicy())
                    .build();

            CtcCalculationOutput out = service.calculate(in);
            // 30 000 × 29.5/30 = 29 500
            assertThat(out.getGrossEarnings()).isEqualByComparingTo("29500.00");
            // TDS = 29 500 × 5% = 1 475
            assertThat(out.getTdsAmount()).isEqualByComparingTo("1475.00");
            assertThat(out.getNetPay()).isEqualByComparingTo("28025.00");
        }
    }

    // ------------------------------------------------------------------
    // Null-safety and zero handling
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Null safety")
    class NullSafety {

        @Test
        @DisplayName("Null lopDays is treated as zero (no deduction)")
        void nullLopTreatedAsZero() {
            CtcCalculationInput in = CtcCalculationInput.builder()
                    .structureType(SalaryStructureType.CONTRACT)
                    .monthlyGrossCtc(new BigDecimal("10000"))
                    .workingDays(30)
                    .lopDays(null)
                    .policy(defaultPolicy())
                    .build();

            CtcCalculationOutput out = service.calculate(in);
            assertThat(out.getLopDeduction()).isEqualByComparingTo("0.00");
            assertThat(out.getGrossEarnings()).isEqualByComparingTo("10000.00");
        }

        @Test
        @DisplayName("Null incentives/adjustments are treated as zero")
        void nullIncentivesAndAdjustments() {
            CtcCalculationInput in = CtcCalculationInput.builder()
                    .structureType(SalaryStructureType.MANAGEMENT)
                    .monthlyGrossCtc(new BigDecimal("50000"))
                    .workingDays(30)
                    .incentives(null)
                    .adjustments(null)
                    .policy(defaultPolicy())
                    .build();

            CtcCalculationOutput out = service.calculate(in);
            assertThat(out.getIncentives()).isEqualByComparingTo("0.00");
            assertThat(out.getAdjustments()).isEqualByComparingTo("0.00");
            assertThat(out.getNetPay()).isEqualByComparingTo("48050.00");
        }

        @Test
        @DisplayName("Zero gross yields zero net")
        void zeroGross() {
            CtcCalculationInput in = CtcCalculationInput.builder()
                    .structureType(SalaryStructureType.MANAGEMENT)
                    .monthlyGrossCtc(BigDecimal.ZERO)
                    .workingDays(30)
                    .policy(defaultPolicy())
                    .build();

            CtcCalculationOutput out = service.calculate(in);
            assertThat(out.getGrossEarnings()).isEqualByComparingTo("0.00");
            // Gross is zero but fixed PF still comes out of the (zero) pay.
            assertThat(out.getEmployeePf()).isEqualByComparingTo("1950.00");
            assertThat(out.getNetPay()).isEqualByComparingTo("-1950.00");
        }
    }

    // ------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Input validation")
    class Validation {

        @Test
        @DisplayName("Null input throws BadRequestException")
        void nullInput() {
            assertThatThrownBy(() -> service.calculate(null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("Missing structureType throws")
        void missingStructureType() {
            CtcCalculationInput in = CtcCalculationInput.builder()
                    .monthlyGrossCtc(new BigDecimal("50000"))
                    .workingDays(30)
                    .policy(defaultPolicy())
                    .build();

            assertThatThrownBy(() -> service.calculate(in))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("structureType");
        }

        @Test
        @DisplayName("Negative gross rejected")
        void negativeGross() {
            CtcCalculationInput in = CtcCalculationInput.builder()
                    .structureType(SalaryStructureType.CONTRACT)
                    .monthlyGrossCtc(new BigDecimal("-1"))
                    .workingDays(30)
                    .policy(defaultPolicy())
                    .build();

            assertThatThrownBy(() -> service.calculate(in))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("monthlyGrossCtc");
        }

        @Test
        @DisplayName("Zero or negative working days rejected")
        void nonPositiveWorkingDays() {
            CtcCalculationInput in = CtcCalculationInput.builder()
                    .structureType(SalaryStructureType.CONTRACT)
                    .monthlyGrossCtc(new BigDecimal("50000"))
                    .workingDays(0)
                    .policy(defaultPolicy())
                    .build();

            assertThatThrownBy(() -> service.calculate(in))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("workingDays");
        }

        @Test
        @DisplayName("Missing policy rejected")
        void missingPolicy() {
            CtcCalculationInput in = CtcCalculationInput.builder()
                    .structureType(SalaryStructureType.CONTRACT)
                    .monthlyGrossCtc(new BigDecimal("50000"))
                    .workingDays(30)
                    .policy(null)
                    .build();

            assertThatThrownBy(() -> service.calculate(in))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("policy");
        }

        @Test
        @DisplayName("Negative LOP rejected")
        void negativeLop() {
            CtcCalculationInput in = CtcCalculationInput.builder()
                    .structureType(SalaryStructureType.CONTRACT)
                    .monthlyGrossCtc(new BigDecimal("50000"))
                    .workingDays(30)
                    .lopDays(new BigDecimal("-1"))
                    .policy(defaultPolicy())
                    .build();

            assertThatThrownBy(() -> service.calculate(in))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("lopDays");
        }
    }

    // ------------------------------------------------------------------
    // Scale / rounding
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Output is always scaled to policy.outputScale (HALF_UP)")
    void outputScaleRespected() {
        // 1 000 / 3 = 333.333… — output scale 2 should give 333.33.
        Policy policy = Policy.builder()
                .defaultEmployerPf(BigDecimal.ZERO)
                .defaultEmployeePf(BigDecimal.ZERO)
                .defaultLwf(BigDecimal.ZERO)
                .contractTdsRatePercent(new BigDecimal("33.3333"))
                .precisionScale(4)
                .outputScale(2)
                .build();

        CtcCalculationInput in = CtcCalculationInput.builder()
                .structureType(SalaryStructureType.CONTRACT)
                .monthlyGrossCtc(new BigDecimal("1000"))
                .workingDays(30)
                .policy(policy)
                .build();

        CtcCalculationOutput out = service.calculate(in);
        // 1 000 × 33.3333% = 333.333 → rounded HALF_UP to 2 = 333.33
        assertThat(out.getTdsAmount().scale()).isEqualTo(2);
        assertThat(out.getTdsAmount()).isEqualByComparingTo("333.33");
        assertThat(out.getNetPay()).isEqualByComparingTo("666.67");
    }
}
