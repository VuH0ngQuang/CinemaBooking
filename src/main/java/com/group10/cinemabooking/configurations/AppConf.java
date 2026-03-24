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
    private Jwt jwt;
    private PaySecret paySecret;
    private Minio minio;

    @PostConstruct
    public void init() {
        log.info("===== Application Configuration =====");
        log.info("Database URL: {}", database.getUrl());
        log.info("Database Username: {}", database.getUsername());
        log.info("JWT Expiration (ms): {}", jwt.getExpirationMs());
        log.info("=====================================");
    }

    @Data
    public static class Database {
        private String url;
        private String username;
        private String password;
    }

    @Data
    public static class Jwt {
        private String secret;
        private long expirationMs;
    }

    @Data
    public static class PaySecret {
        private String clientId;
        private String apiKey;
        private String checksumKey;
    }

    @Data
    public static class Minio {
        private String url;
        private String accessKey;
        private String secretKey;
        private String bucket;
    }
}
