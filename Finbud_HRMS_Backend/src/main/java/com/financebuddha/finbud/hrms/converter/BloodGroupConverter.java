package com.financebuddha.finbud.hrms.converter;

import com.financebuddha.finbud.hrms.enums.BloodGroup;
import jakarta.persistence.Converter;

/**
 * Tolerant converter for {@link BloodGroup}. Mirrors
 * {@code EmployeeImportServiceImpl.normalizeBloodGroup} so clinical notation
 * such as {@code "O+"}, {@code "A-"} or {@code "AB Positive"} maps to the
 * canonical constants ({@code O_POSITIVE}, {@code A_NEGATIVE}, ...). Unparseable
 * values degrade to {@link BloodGroup#UNKNOWN} rather than crashing the query.
 */
@Converter
public class BloodGroupConverter extends TolerantEnumConverter<BloodGroup> {

    public BloodGroupConverter() {
        super(BloodGroup.class);
    }

    @Override
    protected BloodGroup synonym(String raw, String normalised) {
        String s = raw.toUpperCase().replaceAll("\\s+", "");

        String sign = null;
        if (s.endsWith("+") || s.endsWith("POSITIVE") || s.endsWith("POS")) {
            sign = "POSITIVE";
        } else if (s.endsWith("-") || s.endsWith("NEGATIVE") || s.endsWith("NEG")) {
            sign = "NEGATIVE";
        }

        String letters = s.replaceAll("[^A-Z]", "")
                .replace("POSITIVE", "")
                .replace("NEGATIVE", "")
                .replace("POS", "")
                .replace("NEG", "");

        if (sign == null || letters.isEmpty()) {
            return null;
        }
        try {
            return BloodGroup.valueOf(letters + "_" + sign);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    protected BloodGroup fallback() {
        return BloodGroup.UNKNOWN;
    }
}
