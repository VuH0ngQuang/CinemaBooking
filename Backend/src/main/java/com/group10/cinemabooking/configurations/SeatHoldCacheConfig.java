package com.group10.cinemabooking.configurations;

import com.group10.cinemabooking.models.cache.SeatHoldCacheEntry;
import com.group10.cinemabooking.utils.InAppCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeatHoldCacheConfig {

    @Bean(name = "seatHoldCache")
    public InAppCache<String, SeatHoldCacheEntry> seatHoldCache() {
        return new InAppCache<>();
    }
}