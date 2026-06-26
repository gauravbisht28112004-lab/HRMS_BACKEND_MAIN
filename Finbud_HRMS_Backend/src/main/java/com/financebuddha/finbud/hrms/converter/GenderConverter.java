package com.financebuddha.finbud.hrms.converter;

import com.financebuddha.finbud.hrms.enums.Gender;
import jakarta.persistence.Converter;

/**
 * Tolerant converter for {@link Gender}. Mirrors
 * {@code EmployeeImportServiceImpl.normalizeGender} so DB values like
 * {@code "Male"}, {@code "M"}, {@code "female"} or {@code "Prefer not to say"}
 * load instead of crashing the employee list query.
 */
@Converter
public class GenderConverter extends TolerantEnumConverter<Gender> {

    public GenderConverter() {
        super(Gender.class);
    }

    @Override
    protected Gender synonym(String raw, String normalised) {
        if (normalised.startsWith("M")) {
            return Gender.MALE;
        }
        if (normalised.startsWith("F")) {
            return Gender.FEMALE;
        }
        if (normalised.contains("NOT")) {
            return Gender.PREFER_NOT_TO_SAY;
        }
        if (normalised.startsWith("O")) {
            return Gender.OTHER;
        }
        return null;
    }
}
