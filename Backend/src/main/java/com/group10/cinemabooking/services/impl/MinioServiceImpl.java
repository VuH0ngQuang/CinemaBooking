package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.dtos.ImgUrlDto;
import io.minio.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.group10.cinemabooking.configurations.AppConf;
import com.group10.cinemabooking.exception.InvalidRequestException;
import com.group10.cinemabooking.services.MinioService;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.errors.MinioException;
import io.minio.MinioClient;

import java.lang.reflect.Method;
import java.util.Map;

@Service
public class MinioServiceImpl implements MinioService {
    private static final Logger log = LoggerFactory.getLogger(MinioServiceImpl.class);
    private final MinioClient minioClient;
    private final AppConf appConf;

    @Autowired
    public MinioServiceImpl(MinioClient minioClient, AppConf appConf) {
        this.minioClient = minioClient;
        this.appConf = appConf;
    }

    @Override
    public ImgUrlDto uploadImage(Long id) {
        if (id == null) {
            throw new InvalidRequestException("Image id must not be null");
        }
        try {
            String bucket = appConf.getMinio().getBucket();

            String horizontalUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.PUT)
                            .bucket(bucket)
                            .object("poster/horizontal/" + id + ".jpg")
                            .expiry(60 * 10)
                            .build()
            );

            String verticalUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.PUT)
                            .bucket(bucket)
                            .object("poster/vertical/" + id + ".jpg")
                            .expiry(60 * 10)
                            .build()
            );

            return ImgUrlDto.builder()
                    .horizontal(horizontalUrl)
                    .vertical(verticalUrl)
                    .build();
        } catch (MinioException e) {
            log.error("Error generating image upload URLs: {}", e.getMessage());
            log.error("HTTP Trace: {}", e.httpTrace());
            throw new RuntimeException("Failed to generate Minio upload URLs", e);
        }
    }
}
