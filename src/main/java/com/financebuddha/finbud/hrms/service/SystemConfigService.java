package com.financebuddha.finbud.hrms.service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Typed accessor over the {@code system_config} table.
 * <p>
 * Prefer this service over direct {@code SystemConfigRepository} usage in
 * domain code — it coerces values to the right type, supplies sensible
 * defaults, and caches reads to avoid one round-trip per payroll line
 * during bulk generation. Canonical config keys are defined as constants
 * on {@link Keys} below.
 */
public interface SystemConfigService {

    /** Well-known config keys seeded by Flyway V5. Keep in sync with the migration. */
    final class Keys {
        private Keys() {}

        public static final String PAYROLL_PF_EMPLOYER_DEFAULT      = "payroll.pf.employer_default";
        public static final String PAYROLL_PF_EMPLOYEE_DEFAULT      = "payroll.pf.employee_default";
        public static final String PAYROLL_LWF_DEFAULT              = "payroll.lwf.default";
        public static final String PAYROLL_TDS_CONTRACT_RATE_PCT    = "payroll.tds.contract_rate_percent";
        public static final String PAYROLL_CALC_PRECISION_SCALE     = "payroll.calc.precision_scale";
        public static final String PAYROLL_CALC_OUTPUT_SCALE        = "payroll.calc.output_scale";

        public static final String AUTH_DEFAULT_PASSWORD            = "auth.default_password";
        public static final String AUTH_DEFAULT_ROLE                = "auth.default_role";

        public static final String IMPORT_EMPLOYEE_DEFAULT_COUNTRY  = "import.employee.default_country";
        public static final String IMPORT_EMPLOYEE_DEFAULT_LOCATION = "import.employee.default_location";

        public static final String ATTENDANCE_DEVICE_API_KEY_HEADER = "attendance.device.api_key_header";
        public static final String ATTENDANCE_DEVICE_API_KEY        = "attendance.device.api_key";
    }

    Optional<String> get(String key);

    String getOrDefault(String key, String fallback);

    /** Reads an integer config value; returns {@code fallback} if missing or unparseable. */
    int getInt(String key, int fallback);

    /** Reads a BigDecimal config value; returns {@code fallback} if missing or unparseable. */
    BigDecimal getBigDecimal(String key, BigDecimal fallback);

    /** Reads a boolean config value (true/yes/1 are truthy); returns {@code fallback} if missing. */
    boolean getBoolean(String key, boolean fallback);

    /**
     * Upserts a config entry. The in-memory cache for {@code key} is invalidated
     * so subsequent reads observe the new value within the same JVM.
     */
    void set(String key, String value, String description);

    /** Invalidates all cached entries — use after bulk SQL updates from admin tooling. */
    void invalidateAll();
}
