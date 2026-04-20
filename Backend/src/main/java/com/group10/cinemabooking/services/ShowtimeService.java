package com.group10.cinemabooking.services;

import com.group10.cinemabooking.dtos.ShowtimeDto;

import java.util.List;

public interface ShowtimeService {

    ShowtimeDto createShowtime(ShowtimeDto showtimeDto);

    List<ShowtimeDto> getAllShowtimes();

    List<ShowtimeDto> getAllByMovieId(Long movieId);

    ShowtimeDto getShowtimeById(Long showtimeId);

    ShowtimeDto updateShowtime(Long showtimeId, ShowtimeDto showtimeDto);

    void deleteShowtime(Long showtimeId);
}