package com.advertmarket.shared.metric;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class MetricsFacadeTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final MetricsFacade facade = new MetricsFacade(registry);

    @Test
    void incrementCounter_createsAndIncrements() {
        facade.incrementCounter("test.counter", "key", "value");
        facade.incrementCounter("test.counter", "key", "value");

        var counter = registry.find("test.counter")
                .tag("key", "value").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    void incrementCounter_separateTagsCreateSeparateCounters() {
        facade.incrementCounter("test.counter", "k", "a");
        facade.incrementCounter("test.counter", "k", "b");

        assertThat(registry.find("test.counter")
                .tag("k", "a").counter().count()).isEqualTo(1.0);
        assertThat(registry.find("test.counter")
                .tag("k", "b").counter().count()).isEqualTo(1.0);
    }

    @Test
    void recordTimer_measuresSupplier() {
        String result = facade.recordTimer("test.timer",
                () -> "done", "op", "test");

        assertThat(result).isEqualTo("done");
        var timer = registry.find("test.timer")
                .tag("op", "test").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void recordTimer_measuresRunnable() {
        facade.recordTimer("test.runnable", () -> {});

        var timer = registry.find("test.runnable").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void registry_returnsUnderlying() {
        assertThat(facade.registry()).isSameAs(registry);
    }
}
