package com.group10.cinemabooking.utils;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class LockManager<K> {

    private final ConcurrentHashMap<K, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLock getLock(K key) {
        return locks.computeIfAbsent(key, k -> new ReentrantLock());
    }
}