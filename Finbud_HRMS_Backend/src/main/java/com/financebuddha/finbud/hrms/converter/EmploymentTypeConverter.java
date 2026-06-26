package com.financebuddha.finbud.hrms.converter;

import com.financebuddha.finbud.hrms.enums.EmploymentType;
import jakarta.persistence.Converter;

/**
 * Tolerant converter for {@link EmploymentType}. Mirrors
 * {@code EmployeeImportServiceImpl.normalizeEmploymentType}: free-text values
 * like {@code "Full Time"}, {@code "Part-time"} or {@code "Contractor"} resolve,
 * and anything unrecognised defaults to {@link EmploymentType#FULL_TIME} (the
 * same default the entity and importer use).
 */
@Converter
public class EmploymentTypeConverter extends TolerantEnumConverter<EmploymentType> {

    public EmploymentTypeConverter() {
        super(EmploymentType.class);
    }

    @Override
    protected EmploymentType synonym(String raw, String normalised) {
        if (normalised.contains("PART")) {
            return EmploymentType.PART_TIME;
        }
        if (normalised.contains("CONTRACT")) {
            return EmploymentType.CONTRACT;
        }
        if (normalised.contains("INTERN")) {
            return EmploymentType.INTERN;
        }
        if (normalised.contains("PROB")) {
            return EmploymentType.PROBATION;
        }
        return null;
    }

    @Override
    protected EmploymentType fallback() {
        return EmploymentType.FULL_TIME;
    }
}
