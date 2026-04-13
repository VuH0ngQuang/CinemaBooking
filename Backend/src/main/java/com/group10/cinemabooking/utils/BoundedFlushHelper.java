package com.group10.cinemabooking.utils;

import jakarta.persistence.EntityManager;

public class BoundedFlushHelper {
    private final EntityManager entityManager;
    private final int maxWritesPerFlush;
    private final long maxIntervalMillis;
    private int pendingWrites;
    private long lastFlushNanos;

    public BoundedFlushHelper(EntityManager entityManager, int maxWritesPerFlush, long maxIntervalMillis) {
        this.entityManager = entityManager;
        this.maxWritesPerFlush = maxWritesPerFlush;
        this.maxIntervalMillis = maxIntervalMillis;
        this.pendingWrites = 0;
        this.lastFlushNanos = System.nanoTime();
    }

    public void onWrite() {
        pendingWrites++;
        flushIfNeeded();
    }

    public void forceFlush() {
        if (pendingWrites <= 0) {
            return;
        }

        entityManager.flush();
        pendingWrites = 0;
        lastFlushNanos = System.nanoTime();
    }

    private void flushIfNeeded() {
        if (pendingWrites >= maxWritesPerFlush || elapsedMillisSinceLastFlush() >= maxIntervalMillis) {
            entityManager.flush();
            pendingWrites = 0;
            lastFlushNanos = System.nanoTime();
        }
    }

    private long elapsedMillisSinceLastFlush() {
        return (System.nanoTime() - lastFlushNanos) / 1_000_000L;
    }
}
