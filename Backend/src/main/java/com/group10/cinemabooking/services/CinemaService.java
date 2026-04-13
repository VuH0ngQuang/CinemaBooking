package com.group10.cinemabooking.services;

import com.group10.cinemabooking.dtos.CinemaDto;
import com.group10.cinemabooking.dtos.CinemaRequestDto;

import java.util.List;

public interface CinemaService {
    CinemaDto createCinema(CinemaRequestDto requestDto);
    List<CinemaDto> getAllCinemas();
    CinemaDto getCinemaById(Long cinemaId);
    CinemaDto updateCinema(Long cinemaId, CinemaRequestDto requestDto);
    void deleteCinema(Long cinemaId);
}