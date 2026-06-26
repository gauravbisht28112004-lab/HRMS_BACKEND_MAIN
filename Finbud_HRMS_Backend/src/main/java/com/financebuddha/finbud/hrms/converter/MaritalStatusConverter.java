package com.financebuddha.finbud.hrms.converter;

import com.financebuddha.finbud.hrms.enums.MaritalStatus;
import jakarta.persistence.Converter;

/**
 * Tolerant converter for {@link MaritalStatus}. Generic normalisation already
 * resolves the common title-case inputs ({@code "Married"} -> MARRIED,
 * {@code "Single"} -> SINGLE); the synonym hook covers a few free-text variants.
 */
@Converter
public class MaritalStatusConverter extends TolerantEnumConverter<MaritalStatus> {

    public MaritalStatusConverter() {
        super(MaritalStatus.class);
    }

    @Override
    protected MaritalStatus synonym(String raw, String normalised) {
        if (normalised.startsWith("MARR")) {
            return MaritalStatus.MARRIED;
        }
        if (normalised.startsWith("SING") || normalised.startsWith("UNMARR")) {
            return MaritalStatus.SINGLE;
        }
        if (normalised.startsWith("DIVOR")) {
            return MaritalStatus.DIVORCED;
        }
        if (normalised.startsWith("WIDOW")) {
            return MaritalStatus.WIDOWED;
        }
        if (normalised.startsWith("SEPAR")) {
            return MaritalStatus.SEPARATED;
        }
        return null;
    }
}
