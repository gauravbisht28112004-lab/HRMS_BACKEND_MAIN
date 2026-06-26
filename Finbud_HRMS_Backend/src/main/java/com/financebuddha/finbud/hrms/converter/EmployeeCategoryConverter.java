package com.financebuddha.finbud.hrms.converter;

import com.financebuddha.finbud.hrms.enums.EmployeeCategory;
import jakarta.persistence.Converter;

/**
 * Tolerant converter for {@link EmployeeCategory}. Mirrors
 * {@code EmployeeImportServiceImpl.normalizeEmployeeCategory}; note the source
 * label "Contract" maps to {@link EmployeeCategory#CONTRACT_EMPLOYEE}, which the
 * generic normaliser alone cannot reach. Nullable column, so no fallback.
 */
@Converter
public class EmployeeCategoryConverter extends TolerantEnumConverter<EmployeeCategory> {

    public EmployeeCategoryConverter() {
        super(EmployeeCategory.class);
    }

    @Override
    protected EmployeeCategory synonym(String raw, String normalised) {
        if (normalised.contains("CONTRACT")) {
            return EmployeeCategory.CONTRACT_EMPLOYEE;
        }
        if (normalised.contains("INTERN")) {
            return EmployeeCategory.INTERN;
        }
        if (normalised.contains("CONSULT")) {
            return EmployeeCategory.CONSULTANT;
        }
        if (normalised.contains("PERMAN") || normalised.contains("REGULAR")) {
            return EmployeeCategory.PERMANENT;
        }
        return null;
    }
}
