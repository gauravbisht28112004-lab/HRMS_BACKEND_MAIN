package com.financebuddha.finbud.hrms.converter;

import com.financebuddha.finbud.hrms.enums.BackgroundCheckStatus;
import jakarta.persistence.Converter;

/**
 * Tolerant converter for {@link BackgroundCheckStatus}. Mirrors
 * {@code EmployeeImportServiceImpl.normalizeBackgroundCheck}, including the
 * "N/A" -&gt; {@link BackgroundCheckStatus#NOT_APPLICABLE} shorthand.
 */
@Converter
public class BackgroundCheckStatusConverter extends TolerantEnumConverter<BackgroundCheckStatus> {

    public BackgroundCheckStatusConverter() {
        super(BackgroundCheckStatus.class);
    }

    @Override
    protected BackgroundCheckStatus synonym(String raw, String normalised) {
        if (normalised.contains("PROGRESS")) {
            return BackgroundCheckStatus.IN_PROGRESS;
        }
        if (normalised.contains("COMPLET")) {
            return BackgroundCheckStatus.COMPLETED;
        }
        if (normalised.contains("FAIL")) {
            return BackgroundCheckStatus.FAILED;
        }
        if (normalised.contains("NOT_APPLIC") || normalised.equals("NA")
                || raw.equalsIgnoreCase("n/a")) {
            return BackgroundCheckStatus.NOT_APPLICABLE;
        }
        if (normalised.contains("NOT_INIT") || normalised.contains("PENDING")) {
            return BackgroundCheckStatus.NOT_INITIATED;
        }
        return null;
    }
}
