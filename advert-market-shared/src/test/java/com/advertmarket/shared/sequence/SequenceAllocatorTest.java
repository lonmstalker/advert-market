package com.advertmarket.shared.sequence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Sequence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SequenceAllocator — bulk sequence prefetch")
class SequenceAllocatorTest {

    private DSLContext dsl;
    private Sequence<Long> sequence;
    private SequenceAllocator allocator;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        dsl = mock(DSLContext.class);
        sequence = mock(Sequence.class);
        when(sequence.getName()).thenReturn("deal_subwallet_seq");
    }

    @Test
    @DisplayName("Should allocate values from prefetched batch without extra DB calls")
    void allocatesFromBatch() {
        allocator = new SequenceAllocator(dsl, sequence, 5);
        mockBatch(List.of(10L, 11L, 12L, 13L, 14L));

        assertThat(allocator.next()).isEqualTo(10L);
        assertThat(allocator.next()).isEqualTo(11L);
        assertThat(allocator.next()).isEqualTo(12L);
        assertThat(allocator.next()).isEqualTo(13L);
        assertThat(allocator.next()).isEqualTo(14L);

        verify(dsl, times(1)).fetch(any(String.class), any(), any());
    }

    @Test
    @DisplayName("Should fetch new batch when current batch is exhausted")
    void fetchesNewBatchWhenExhausted() {
        allocator = new SequenceAllocator(dsl, sequence, 3);
        mockBatch(List.of(1L, 2L, 3L), List.of(4L, 5L, 6L));

        for (int i = 0; i < 3; i++) {
            allocator.next();
        }
        assertThat(allocator.next()).isEqualTo(4L);

        verify(dsl, times(2)).fetch(any(String.class), any(), any());
    }

    @Test
    @DisplayName("Should produce unique values under concurrent access")
    void concurrentAccess() throws InterruptedException {
        allocator = new SequenceAllocator(dsl, sequence, 50);
        mockBatchSequential(50, 10);

        int threads = 10;
        int perThread = 50;
        Set<Long> values = ConcurrentHashMap.newKeySet();
        var latch = new CountDownLatch(threads);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int t = 0; t < threads; t++) {
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < perThread; i++) {
                            values.add(allocator.next());
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }

        assertThat(values).hasSize(threads * perThread);
    }

    @SuppressWarnings("unchecked")
    private void mockBatch(List<Long>... batches) {
        // Prepare result mocks first to avoid nested stubbing
        Result<Record>[] results = new Result[batches.length];
        for (int i = 0; i < batches.length; i++) {
            results[i] = mock(Result.class);
            when(results[i].getValues(0, Long.class)).thenReturn(batches[i]);
        }
        var stub = when(dsl.fetch(any(String.class), any(), any()));
        for (Result<Record> result : results) {
            stub = stub.thenReturn(result);
        }
    }

    @SuppressWarnings("unchecked")
    private void mockBatchSequential(int batchSize, int batchCount) {
        // Prepare result mocks first to avoid nested stubbing
        Result<Record>[] results = new Result[batchCount];
        for (int b = 0; b < batchCount; b++) {
            long start = (long) b * batchSize;
            List<Long> batch = java.util.stream.LongStream.range(start, start + batchSize)
                    .boxed().toList();
            results[b] = mock(Result.class);
            when(results[b].getValues(0, Long.class)).thenReturn(batch);
        }
        var stub = when(dsl.fetch(any(String.class), any(), any()));
        for (Result<Record> result : results) {
            stub = stub.thenReturn(result);
        }
    }
}
