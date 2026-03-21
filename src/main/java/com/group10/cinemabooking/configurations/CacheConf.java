package com.group10.cinemabooking.configurations;

import com.group10.cinemabooking.models.Users;
import com.group10.cinemabooking.utils.InAppCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConf {

    @Bean
    public InAppCache<Long, Users> userCache() {
        return new InAppCache<>();
    }

    @Bean
    public InAppCache<String, String> tokenCache() {
        return new InAppCache<>();
    }

    @Bean InAppCache<String, Long> emailCache() {return new InAppCache<>();}
}