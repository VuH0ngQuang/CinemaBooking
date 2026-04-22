package com.group10.cinemabooking.controller;

import com.group10.cinemabooking.dtos.ImgUrlDto;
import com.group10.cinemabooking.dtos.MovieDto;
import com.group10.cinemabooking.services.MovieService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MoviesController {

    private final MovieService movieService;

    public MoviesController(MovieService movieService) {
        this.movieService = movieService;
    }

    @PostMapping
    public ResponseEntity<ImgUrlDto> createMovie(@Valid @RequestBody MovieDto movieDto) {
        ImgUrlDto imgUrlDto = movieService.createMovie(movieDto);
        return new ResponseEntity<>(imgUrlDto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<MovieDto>> getAllMovies() {
        List<MovieDto> movies = movieService.getAllMovies();
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieDto> getMovieById(@PathVariable("id") Long movieId) {
        MovieDto movie = movieService.getMovieById(movieId);
        return ResponseEntity.ok(movie);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MovieDto> updateMovie(@PathVariable("id") Long movieId,
                                              @Valid @RequestBody MovieDto movieDto) {
        MovieDto updatedMovie = movieService.updateMovie(movieId, movieDto);
        return ResponseEntity.ok(updatedMovie);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMovie(@PathVariable("id") Long movieId) {
        movieService.deleteMovie(movieId);
        return ResponseEntity.ok("Movie deleted successfully with id: " + movieId);
    }
}