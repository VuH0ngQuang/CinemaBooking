package com.group10.cinemabooking.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;

@Configuration
public class MinioConf {
    private final AppConf appConf;

    @Autowired
    public MinioConf(AppConf appConf) {
        this.appConf = appConf;
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
        .endpoint(appConf.getMinio().getUrl())
        .credentials(appConf.getMinio().getAccessKey(), appConf.getMinio().getSecretKey())
        .build();
    }
}
