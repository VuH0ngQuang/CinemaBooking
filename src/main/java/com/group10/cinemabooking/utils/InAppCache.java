package com.group10.cinemabooking.utils;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class InAppCache<K, V> {

    private final ConcurrentHashMap<K, V> store = new ConcurrentHashMap<>();

    /**
     * Get value if present, otherwise return null.
     */
    public V get(K key) {
        return store.get(key);
    }

    /**
     * Get value or atomically load and cache it.
     * Prevents cache stampede.
     */
    public V getOrLoad(K key, Function<K, V> loader) {
        V cached = store.get(key);
        if (cached != null) {
            return cached;
        }
        if (isTransactionActive()) {
            V loaded = loader.apply(key);
            if (loaded != null) {
                runAfterCommitOrNow(() -> store.put(key, loaded));
            }
            return loaded;
        }
        return store.computeIfAbsent(key, loader);
    }

    /**
     * Manually insert or update value.
     */
    public void put(K key, V value) {
        runAfterCommitOrNow(() -> store.put(key, value));
    }

    /**
     * Remove specific key.
     */
    public void remove(K key) {
        runAfterCommitOrNow(() -> store.remove(key));
    }

    /**
     * Clear entire cache.
     */
    public void clear() {
        runAfterCommitOrNow(store::clear);
    }

    /**
     * Current cache size.
     */
    public int size() {
        return store.size();
    }

    /**
     * Check existence.
     */
    public boolean contains(K key) {
        return store.containsKey(key);
    }

    private boolean isTransactionActive() {
        return TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive();
    }

    private void runAfterCommitOrNow(Runnable action) {
        if (isTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }
}