package com.group10.cinemabooking.configurations;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppConf {
    private static final Logger log = LoggerFactory.getLogger(AppConf.class);
    private Database database;

    @PostConstruct
    public void init() {
        log.info("===== Application Configuration =====");
        log.info("Database URL: {}", database.getUrl());
        log.info("Database Username: {}", database.getUsername());
        log.info("=====================================");
    }

    @Data
    public static class Database {
        private String url;
        private String username;
        private String password;
    }
}
