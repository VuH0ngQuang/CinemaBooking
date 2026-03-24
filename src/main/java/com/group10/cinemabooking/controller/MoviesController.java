package com.group10.cinemabooking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.group10.cinemabooking.services.MinioService;

@RestController
@RequestMapping("/api/movies")
public class MoviesController {
    public final MinioService minioService;

    @Autowired
    public MoviesController(MinioService minioService) {
        this.minioService = minioService;
    }

    @PostMapping("/upload-poster/{id}")
    public ResponseEntity<String> uploadPoster(@PathVariable("id") String id) {
        return ResponseEntity.ok(minioService.uploadImage(id));
    }
}
