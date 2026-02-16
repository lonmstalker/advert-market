package com.advertmarket.shared.metric;

import com.advertmarket.shared.FenumGroup;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.fenum.qual.Fenum;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Facade over Micrometer {@link MeterRegistry} with cached counters and timers.
 */
@RequiredArgsConstructor
@SuppressWarnings("fenum:argument")
public class MetricsFacade {

    private final MeterRegistry registry;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();

    /** Increments a named counter by 1 with optional tags. */
    public void incrementCounter(
            @Fenum(FenumGroup.METRIC_NAME) @NonNull String name,
            String... tags) {
        getOrCreateCounter(name, tags).increment();
    }

    /** Increments a named counter by the given amount with optional tags. */
    public void incrementCounter(
            @Fenum(FenumGroup.METRIC_NAME) @NonNull String name,
            double amount,
            String... tags) {
        getOrCreateCounter(name, tags).increment(amount);
    }

    /** Records timer around a supplier with optional tags. */
    public <T> T recordTimer(
            @Fenum(FenumGroup.METRIC_NAME) @NonNull String name,
            @NonNull Supplier<T> supplier,
            String... tags) {
        return getOrCreateTimer(name, tags).record(supplier);
    }

    /** Records timer around a runnable with optional tags. */
    public void recordTimer(
            @Fenum(FenumGroup.METRIC_NAME) @NonNull String name,
            @NonNull Runnable runnable,
            String... tags) {
        getOrCreateTimer(name, tags).record(runnable);
    }

    /** Registers a gauge backed by the given {@link Number}. */
    public <T extends Number> void registerGauge(
            @Fenum(FenumGroup.METRIC_NAME) @NonNull String name,
            @NonNull T number) {
        registry.gauge(name, number);
    }

    /** Returns the underlying registry for advanced use cases. */
    @NonNull
    public MeterRegistry registry() {
        return registry;
    }

    private Counter getOrCreateCounter(String name, String... tags) {
        return counters.computeIfAbsent(
                cacheKey(name, tags),
                _ -> Counter.builder(name).tags(tags).register(registry)
        );
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
