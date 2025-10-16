package com.example.worktime.service;

import com.example.worktime.model.ProcessCode;
import com.example.worktime.repository.ProcessCodeRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service converting process names to codes using the database table.
 */
@Service
public class ProcessCodeService {

    private final ProcessCodeRepository repository;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public ProcessCodeService(ProcessCodeRepository repository) {
        this.repository = repository;
    }

    /**
     * Pre-load all known process name/code pairs into the local cache and
     * return a mutable copy that callers can reuse when resolving names in bulk.
     */
    public Map<String, String> loadCacheSnapshot() {
        List<ProcessCode> all = repository.findAll();
        Map<String, String> snapshot = new HashMap<>();
        for (ProcessCode pc : all) {
            if (pc.getName() == null || pc.getCode() == null) {
                continue;
            }
            String name = pc.getName().trim();
            String code = pc.getCode().trim();
            if (name.isEmpty() || code.isEmpty()) {
                continue;
            }
            cache.put(name, code);
            snapshot.put(name, code);
        }
        return snapshot;
    }

    public Map<String, String> getCachedMappings() {
        return Collections.unmodifiableMap(cache);
    }

    public String getCode(String processName) {
        if (processName == null) return null;
        String name = processName.trim();
        if (name.isEmpty()) return null;
        String existing = cache.get(name);
        if (existing != null) {
            return existing;
        }
        ProcessCode pc = repository.findByName(name);
        if (pc != null && pc.getCode() != null) {
            String code = pc.getCode().trim();
            if (!code.isEmpty()) {
                cache.put(name, code);
                return code;
            }
        }
        return null;
    }
}