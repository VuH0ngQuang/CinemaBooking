package com.group10.cinemabooking.utils;

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
        return store.computeIfAbsent(key, loader);
    }

    /**
     * Manually insert or update value.
     */
    public void put(K key, V value) {
        store.put(key, value);
    }

    /**
     * Remove specific key.
     */
    public void remove(K key) {
        store.remove(key);
    }

    /**
     * Clear entire cache.
     */
    public void clear() {
        store.clear();
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
}