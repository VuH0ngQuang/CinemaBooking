package com.group10.cinemabooking.services;

import com.group10.cinemabooking.dtos.MovieDto;

import java.util.List;

public interface MovieService {

    MovieDto createMovie(MovieDto movieDto);

    List<MovieDto> getAllMovies();

    MovieDto getMovieById(Long movieId);

    MovieDto updateMovie(Long movieId, MovieDto movieDto);

    void deleteMovie(Long movieId);
}