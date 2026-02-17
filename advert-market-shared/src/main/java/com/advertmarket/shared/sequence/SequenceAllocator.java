package com.advertmarket.shared.sequence;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.concurrent.ThreadSafe;
import org.jooq.DSLContext;
import org.jooq.Sequence;
import org.jooq.impl.DSL;

/**
 * Generic bulk sequence allocator that prefetches batches from PostgreSQL sequences.
 *
 * <p>Uses {@link ReentrantLock} instead of {@code synchronized} to avoid
 * pinning virtual threads to platform threads.
 */
@ThreadSafe
public class SequenceAllocator {

    private final DSLContext dsl;
    private final Sequence<Long> sequence;
    private final int allocationSize;

    private final ReentrantLock lock = new ReentrantLock();
    private long nextValue;
    private long ceiling;

    /**
     * Creates a new allocator.
     *
     * @param dsl            jOOQ context for batch fetching
     * @param sequence       PostgreSQL sequence to allocate from
     * @param allocationSize number of values to prefetch per batch
     */
    public SequenceAllocator(
            DSLContext dsl, Sequence<Long> sequence, int allocationSize) {
        this.dsl = dsl;
        this.sequence = sequence;
        this.allocationSize = allocationSize;
    }

    /**
     * Returns the next sequence value, allocating a new batch if needed.
     *
     * @return next sequence value
     */
    public long next() {
        lock.lock();
        try {
            if (nextValue >= ceiling) {
                allocateBatch();
            }
            return nextValue++;
        } finally {
            lock.unlock();
        }
    }

    private void allocateBatch() {
        String sequenceName = sequence.getName();
        List<Long> values = dsl.fetch(
                "SELECT nextval({0}) FROM generate_series(1, {1})",
                DSL.inline(sequenceName),
                DSL.val(allocationSize)
        ).getValues(0, Long.class);

        nextValue = Collections.min(values);
        ceiling = Collections.max(values) + 1;
    }
}
