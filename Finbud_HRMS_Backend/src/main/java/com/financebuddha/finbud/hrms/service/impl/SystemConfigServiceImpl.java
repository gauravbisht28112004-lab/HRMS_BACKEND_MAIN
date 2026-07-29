package com.financebuddha.finbud.hrms.service.impl;

import com.financebuddha.finbud.hrms.entity.SystemConfig;
import com.financebuddha.finbud.hrms.repository.SystemConfigRepository;
import com.financebuddha.finbud.hrms.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation backed by {@link SystemConfigRepository} with a process-local
 * {@link ConcurrentHashMap} cache. Scope is intentionally per-JVM — the system_config
 * table is small and rarely written, so a full invalidate on {@code set()} is cheap.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigRepository repository;

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    // Sentinel used when a lookup misses — lets us cache "not found" decisions
    // without having to store Optional<String> wrappers in the map.
    private static final String MISSING = "\0__MISSING__\0";

    @Override
    @Transactional(readOnly = true)
    public Optional<String> get(String key) {
        String cached = cache.get(key);
        if (cached != null) {
            return MISSING.equals(cached) ? Optional.empty() : Optional.of(cached);
        }
        Optional<String> fromDb = repository.findByConfigKey(key).map(SystemConfig::getConfigValue);
        cache.put(key, fromDb.orElse(MISSING));
        return fromDb;
    }

    @Override
    public String getOrDefault(String key, String fallback) {
        return get(key).filter(v -> !v.isBlank()).orElse(fallback);
    }

    @Override
    public int getInt(String key, int fallback) {
        try {
            return get(key).map(String::trim).filter(v -> !v.isEmpty()).map(Integer::parseInt).orElse(fallback);
        } catch (NumberFormatException e) {
            log.warn("Config key {} has non-integer value; using fallback {}", key, fallback);
            return fallback;
        }
    }

    @Override
    public BigDecimal getBigDecimal(String key, BigDecimal fallback) {
        try {
            return get(key).map(String::trim).filter(v -> !v.isEmpty()).map(BigDecimal::new).orElse(fallback);
        } catch (NumberFormatException e) {
            log.warn("Config key {} has non-numeric value; using fallback {}", key, fallback);
            return fallback;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean fallback) {
        return get(key).map(String::trim).map(String::toLowerCase).map(v ->
                v.equals("true") || v.equals("yes") || v.equals("1") || v.equals("on")
        ).orElse(fallback);
    }

    @Override
    @Transactional
    public void set(String key, String value, String description) {
        SystemConfig config = repository.findByConfigKey(key).orElseGet(() -> {
            SystemConfig created = new SystemConfig();
            created.setConfigKey(key);
            return created;
        });
        config.setConfigValue(value);
        if (description != null) {
            config.setDescription(description);
        }
        repository.save(config);
        cache.put(key, value);
        log.info("SystemConfig updated: {} = {}", key, maskSensitive(key, value));
    }

    @Override
    public void invalidateAll() {
        cache.clear();
        log.info("SystemConfig cache invalidated");
    }

    /** Mask values for keys that look like secrets so we don't leak them via log aggregators. */
    private String maskSensitive(String key, String value) {
        if (value == null || value.isEmpty()) return value;
        String lower = key.toLowerCase();
        if (lower.contains("password") || lower.contains("secret") || lower.contains("api_key") || lower.contains("token")) {
            return "***";
        }
        return value;
    }
}
