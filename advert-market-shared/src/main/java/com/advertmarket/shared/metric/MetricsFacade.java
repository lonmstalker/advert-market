package com.advertmarket.shared.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Facade over Micrometer {@link MeterRegistry} with cached counters and timers.
 */
@Component
public class MetricsFacade {

    private final MeterRegistry registry;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();

    /** Creates a new facade backed by the given registry. */
    public MetricsFacade(MeterRegistry registry) {
        this.registry = registry;
    }

    /** Increments a named counter with optional tags. */
    public void incrementCounter(String name, String... tags) {
        counters.computeIfAbsent(
                cacheKey(name, tags),
                _ -> Counter.builder(name).tags(tags).register(registry)
        ).increment();
    }

    /** Records timer around a supplier with optional tags. */
    public <T> T recordTimer(String name, Supplier<T> supplier,
            String... tags) {
        return getOrCreateTimer(name, tags).record(supplier);
    }

    /** Records timer around a runnable with optional tags. */
    public void recordTimer(String name, Runnable runnable,
            String... tags) {
        getOrCreateTimer(name, tags).record(runnable);
    }

    /** Returns the underlying registry for advanced use cases. */
    public MeterRegistry registry() {
        return registry;
    }

    private Timer getOrCreateTimer(String name, String... tags) {
        return timers.computeIfAbsent(
                cacheKey(name, tags),
                _ -> Timer.builder(name).tags(tags).register(registry)
        );
    }

    private static String cacheKey(String name, String... tags) {
        if (tags.length == 0) {
            return name;
        }
        return name + ":" + String.join(",", tags);
    }
}
