package com.financebuddha.finbud.hrms.converter;

import com.financebuddha.finbud.hrms.enums.ProducerType;
import jakarta.persistence.Converter;

/**
 * Tolerant converter for {@link ProducerType}. Mirrors
 * {@code EmployeeImportServiceImpl.normalizeProducerType}: any value containing
 * "non" maps to {@link ProducerType#NON_PRODUCER}, otherwise PRODUCER.
 */
@Converter
public class ProducerTypeConverter extends TolerantEnumConverter<ProducerType> {

    public ProducerTypeConverter() {
        super(ProducerType.class);
    }

    @Override
    protected ProducerType synonym(String raw, String normalised) {
        if (normalised.contains("NON")) {
            return ProducerType.NON_PRODUCER;
        }
        if (normalised.contains("PRODUCER")) {
            return ProducerType.PRODUCER;
        }
        return null;
    }
}
