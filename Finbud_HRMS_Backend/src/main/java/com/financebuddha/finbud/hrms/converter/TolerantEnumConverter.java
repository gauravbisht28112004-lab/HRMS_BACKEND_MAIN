package com.financebuddha.finbud.hrms.converter;

import jakarta.persistence.AttributeConverter;

/**
 * Base class for tolerant String &lt;-&gt; enum JPA converters.
 *
 * <p><b>Why this exists.</b> Some employee rows were inserted with
 * human-friendly enum values such as {@code "Male"}, {@code "O+"} or
 * {@code "Full Time"} instead of the canonical enum constant names
 * ({@code MALE}, {@code O_POSITIVE}, {@code FULL_TIME}). Hibernate's default
 * {@code @Enumerated(STRING)} hydration calls {@link Enum#valueOf} which throws
 * {@link IllegalArgumentException} on those values — and because the
 * {@code /api/employees} list query hydrates every row, a single bad cell turns
 * the entire endpoint into a 500 (observed:
 * {@code No enum constant ...Gender.Male}).
 *
 * <p>This converter resolves values defensively, in order:
 * <ol>
 *   <li>exact constant-name match;</li>
 *   <li>generic normalisation (trim, upper-case, any run of non-alphanumeric
 *       characters collapsed to a single underscore);</li>
 *   <li>an enum-specific synonym hook;</li>
 *   <li>a fallback value (default {@code null}).</li>
 * </ol>
 *
 * <p>The write path always persists the canonical {@link Enum#name()}, so data
 * self-heals on the next update and stays consistent with repository methods
 * that filter by enum (e.g. {@code findByStatusNot}). Read tolerance is a pure
 * superset of the previous behaviour: every value that used to load still
 * loads; values that used to crash now resolve or degrade gracefully.
 */
public abstract class TolerantEnumConverter<E extends Enum<E>> implements AttributeConverter<E, String> {

    private final Class<E> type;

    protected TolerantEnumConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public E convertToEntityAttribute(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        String raw = dbValue.trim();
        if (raw.isEmpty()) {
            return null;
        }

        // 1. Exact constant name (the common, already-clean case).
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equals(raw)) {
                return constant;
            }
        }

        // 2. Generic normalisation: upper-case and collapse any run of
        //    non-alphanumeric characters into a single underscore, then strip
        //    leading/trailing underscores. Handles "Male" -> MALE,
        //    "Full Time"/"Full-Time" -> FULL_TIME, "Prefer not to say" ->
        //    PREFER_NOT_TO_SAY, etc.
        String normalised = raw.toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equals(normalised)) {
                return constant;
            }
        }

        // 3. Enum-specific synonyms (e.g. "O+" -> O_POSITIVE).
        E synonymMatch = synonym(raw, normalised);
        if (synonymMatch != null) {
            return synonymMatch;
        }

        // 4. Fallback (default null). Never throw — a bad cell must not break
        //    the whole list query.
        return fallback();
    }

    /**
     * Map domain-specific synonyms that the generic normalisation cannot reach.
     *
     * @param raw        the trimmed original DB value
     * @param normalised the generically-normalised form (upper, underscores)
     * @return a matching constant, or {@code null} if none applies
     */
    protected E synonym(String raw, String normalised) {
        return null;
    }

    /**
     * Value used when nothing matches. Defaults to {@code null}; override for
     * enums that have a sensible "unknown"/default constant.
     */
    protected E fallback() {
        return null;
    }
}
