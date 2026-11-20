package com.example.lending.collection.assignment;

import com.example.lending.collection.client.EmployeeClient;
import com.example.lending.collection.config.CollectionProperties;
import com.example.lending.collection.model.CollectorData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Collector roster on top of the employee directory's internal API, cached for the
 * duration set by {@code app.collection.collectors-cache-ttl}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CachedCollectorRoster implements ICollectorRoster {

    private final EmployeeClient employeeClient;
    private final CollectionProperties properties;

    private volatile List<CollectorData> cached = List.of();
    private volatile Instant cachedAt = Instant.EPOCH;

    @Override
    public List<CollectorData> collectors() {
        if (isFresh()) {
            return cached;
        }
        synchronized (this) {
            if (isFresh()) {
                return cached;
            }
            try {
                List<CollectorData> collectors = employeeClient.collectors();
                cached = collectors != null ? collectors : List.of();
                cachedAt = Instant.now();
            } catch (Exception e) {
                // Registry unavailable - keep working on the previous snapshot; assignment
                // must not break enqueuing a case.
                log.error("Failed to fetch the collector roster from the employee directory", e);
            }
            return cached;
        }
    }

    @Override
    public void invalidate() {
        cachedAt = Instant.EPOCH;
    }

    private boolean isFresh() {
        Duration ttl = Duration.ofSeconds(properties.collectorsCacheTtl());
        return !cached.isEmpty() && Instant.now().isBefore(cachedAt.plus(ttl));
    }
}
