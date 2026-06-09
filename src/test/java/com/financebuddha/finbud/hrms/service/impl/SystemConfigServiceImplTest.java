package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.entity.SystemConfig;
import com.financebuddha.finbud.hrms.repository.SystemConfigRepository;
import com.financebuddha.finbud.hrms.service.SystemConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SystemConfigServiceImpl}. Focus areas:
 * <ul>
 *   <li>the process-local cache avoids repeat DB reads (including misses),</li>
 *   <li>typed accessors tolerate whitespace / blank values / parse errors,</li>
 *   <li>{@link SystemConfigService#set(String, String, String)} refreshes the
 *       cache so the new value is visible inside the same JVM,</li>
 *   <li>{@link SystemConfigService#invalidateAll()} forces the next read back
 *       through the repository.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class SystemConfigServiceImplTest {

    @Mock
    private SystemConfigRepository repository;

    @InjectMocks
    private SystemConfigServiceImpl service;

    private SystemConfig cfg(String key, String value) {
        SystemConfig c = new SystemConfig();
        c.setConfigKey(key);
        c.setConfigValue(value);
        return c;
    }

    // ------------------------------------------------------------------
    // Cache behaviour
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Repeat reads of the same key hit the cache, not the DB")
    void readHitsCacheOnSecondCall() {
        when(repository.findByConfigKey("k"))
                .thenReturn(Optional.of(cfg("k", "v")));

        assertThat(service.get("k")).contains("v");
        assertThat(service.get("k")).contains("v");
        assertThat(service.get("k")).contains("v");

        // Only one DB round-trip despite three reads.
        verify(repository, times(1)).findByConfigKey("k");
    }

    @Test
    @DisplayName("Misses are cached via MISSING sentinel — no repeat DB calls")
    void cacheMissesNotRepeated() {
        when(repository.findByConfigKey("absent")).thenReturn(Optional.empty());

        assertThat(service.get("absent")).isEmpty();
        assertThat(service.get("absent")).isEmpty();
        assertThat(service.get("absent")).isEmpty();

        verify(repository, times(1)).findByConfigKey("absent");
    }

    @Test
    @DisplayName("invalidateAll clears cache; next read re-queries the DB")
    void invalidateAllForcesReload() {
        when(repository.findByConfigKey("k"))
                .thenReturn(Optional.of(cfg("k", "v1")))
                .thenReturn(Optional.of(cfg("k", "v2")));

        assertThat(service.get("k")).contains("v1");
        service.invalidateAll();
        assertThat(service.get("k")).contains("v2");

        verify(repository, times(2)).findByConfigKey("k");
    }

    // ------------------------------------------------------------------
    // getOrDefault
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getOrDefault returns stored value when present")
    void getOrDefaultReturnsStored() {
        when(repository.findByConfigKey("k")).thenReturn(Optional.of(cfg("k", "stored")));
        assertThat(service.getOrDefault("k", "fallback")).isEqualTo("stored");
    }

    @Test
    @DisplayName("getOrDefault returns fallback on missing key")
    void getOrDefaultReturnsFallbackOnMiss() {
        when(repository.findByConfigKey("k")).thenReturn(Optional.empty());
        assertThat(service.getOrDefault("k", "fallback")).isEqualTo("fallback");
    }

    @Test
    @DisplayName("getOrDefault treats blank value as missing")
    void getOrDefaultTreatsBlankAsMissing() {
        when(repository.findByConfigKey("k")).thenReturn(Optional.of(cfg("k", "   ")));
        assertThat(service.getOrDefault("k", "fallback")).isEqualTo("fallback");
    }

    // ------------------------------------------------------------------
    // Typed accessors
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getInt parses integers (with whitespace tolerance)")
    void getIntParsesWithWhitespace() {
        when(repository.findByConfigKey("k")).thenReturn(Optional.of(cfg("k", "  42 ")));
        assertThat(service.getInt("k", 0)).isEqualTo(42);
    }

    @Test
    @DisplayName("getInt returns fallback when value is non-numeric")
    void getIntFallsBackOnParseError() {
        when(repository.findByConfigKey("k")).thenReturn(Optional.of(cfg("k", "not-a-number")));
        assertThat(service.getInt("k", 99)).isEqualTo(99);
    }

    @Test
    @DisplayName("getInt returns fallback when key missing")
    void getIntFallsBackOnMiss() {
        when(repository.findByConfigKey("k")).thenReturn(Optional.empty());
        assertThat(service.getInt("k", 7)).isEqualTo(7);
    }

    @Test
    @DisplayName("getBigDecimal parses decimals")
    void getBigDecimalParses() {
        when(repository.findByConfigKey("k")).thenReturn(Optional.of(cfg("k", "1950.00")));
        assertThat(service.getBigDecimal("k", BigDecimal.ZERO))
                .isEqualByComparingTo("1950.00");
    }

    @Test
    @DisplayName("getBigDecimal falls back on parse error")
    void getBigDecimalFallbackOnParseError() {
        when(repository.findByConfigKey("k")).thenReturn(Optional.of(cfg("k", "abc")));
        assertThat(service.getBigDecimal("k", new BigDecimal("5")))
                .isEqualByComparingTo("5");
    }

    @Test
    @DisplayName("getBoolean recognises true/yes/1/on case-insensitively")
    void getBooleanTruthyValues() {
        when(repository.findByConfigKey("true")).thenReturn(Optional.of(cfg("true", "TRUE")));
        when(repository.findByConfigKey("yes")).thenReturn(Optional.of(cfg("yes", "Yes")));
        when(repository.findByConfigKey("one")).thenReturn(Optional.of(cfg("one", "1")));
        when(repository.findByConfigKey("on")).thenReturn(Optional.of(cfg("on", " On ")));

        assertThat(service.getBoolean("true", false)).isTrue();
        assertThat(service.getBoolean("yes", false)).isTrue();
        assertThat(service.getBoolean("one", false)).isTrue();
        assertThat(service.getBoolean("on", false)).isTrue();
    }

    @Test
    @DisplayName("getBoolean treats everything else as false")
    void getBooleanFalsyValues() {
        when(repository.findByConfigKey("off")).thenReturn(Optional.of(cfg("off", "off")));
        assertThat(service.getBoolean("off", true)).isFalse();
    }

    @Test
    @DisplayName("getBoolean returns fallback when key is absent")
    void getBooleanFallback() {
        when(repository.findByConfigKey("k")).thenReturn(Optional.empty());
        assertThat(service.getBoolean("k", true)).isTrue();
    }

    // ------------------------------------------------------------------
    // Upsert / mutation
    // ------------------------------------------------------------------

    @Test
    @DisplayName("set on a new key inserts and caches the value")
    void setCreatesNewEntry() {
        when(repository.findByConfigKey("new.key")).thenReturn(Optional.empty());
        when(repository.save(any(SystemConfig.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.set("new.key", "42", "an integer");

        // Subsequent get() should hit the cache, NOT the DB.
        assertThat(service.get("new.key")).contains("42");
        // findByConfigKey was called exactly once — inside set(); get() should be a cache hit.
        verify(repository, times(1)).findByConfigKey("new.key");
        verify(repository, times(1)).save(any(SystemConfig.class));
    }

    @Test
    @DisplayName("set on an existing key updates and invalidates the cache")
    void setUpdatesExistingEntry() {
        SystemConfig existing = cfg("k", "old");
        when(repository.findByConfigKey("k"))
                .thenReturn(Optional.of(existing))   // read to prime cache
                .thenReturn(Optional.of(existing));  // read inside set()

        when(repository.save(any(SystemConfig.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // prime cache
        assertThat(service.get("k")).contains("old");
        // mutate
        service.set("k", "new", null);
        // cache should now reflect the new value with no extra DB read
        assertThat(service.get("k")).contains("new");

        // Two findByConfigKey calls total: one to prime, one inside set().
        verify(repository, times(2)).findByConfigKey("k");
    }

    @Test
    @DisplayName("set with null description leaves existing description untouched")
    void setPreservesDescriptionOnNull() {
        SystemConfig existing = cfg("k", "v");
        existing.setDescription("original");
        when(repository.findByConfigKey("k")).thenReturn(Optional.of(existing));
        when(repository.save(any(SystemConfig.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.set("k", "v2", null);
        assertThat(existing.getDescription()).isEqualTo("original");
        assertThat(existing.getConfigValue()).isEqualTo("v2");
    }

    // ------------------------------------------------------------------
    // Sanity: well-known keys exist
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Keys constants are stable strings (prevents accidental rename)")
    void keysAreStable() {
        assertThat(SystemConfigService.Keys.PAYROLL_PF_EMPLOYER_DEFAULT).isEqualTo("payroll.pf.employer_default");
        assertThat(SystemConfigService.Keys.PAYROLL_PF_EMPLOYEE_DEFAULT).isEqualTo("payroll.pf.employee_default");
        assertThat(SystemConfigService.Keys.AUTH_DEFAULT_PASSWORD).isEqualTo("auth.default_password");
        assertThat(SystemConfigService.Keys.AUTH_DEFAULT_ROLE).isEqualTo("auth.default_role");
        assertThat(SystemConfigService.Keys.ATTENDANCE_DEVICE_API_KEY).isEqualTo("attendance.device.api_key");
    }
}
