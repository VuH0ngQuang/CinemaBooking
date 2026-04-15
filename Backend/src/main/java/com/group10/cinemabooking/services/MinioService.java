package com.group10.cinemabooking.services;

import com.group10.cinemabooking.dtos.ImgUrlDto;

public interface MinioService {
    ImgUrlDto uploadImage(Long id);
}
