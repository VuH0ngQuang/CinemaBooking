package com.group10.cinemabooking.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.group10.cinemabooking.configurations.AppConf;
import com.group10.cinemabooking.services.MinioService;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http.Method;
import io.minio.errors.MinioException;
import io.minio.MinioClient;

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
    public String uploadImage(String id) {
        String url;
        try {
            url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                .method(Method.PUT)
                .bucket(appConf.getMinio().getBucket())
                .object("poster/"+id+".jpg")
                .expiry(60 * 10) // 10 minutes
                .build());
        } catch (MinioException e) {
            url = "error";
            log.error("Error uploading image to Minio: {}", e.getMessage());
            log.error("HTTP Trace: {}", e.httpTrace());
        }
        return url;
    }
}
