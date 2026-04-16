package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.dtos.ImgUrlDto;
import com.group10.cinemabooking.dtos.MovieDto;
import com.group10.cinemabooking.exception.ResourceNotFoundException;
import com.group10.cinemabooking.models.Movies;
import com.group10.cinemabooking.repository.MovieRepository;
import com.group10.cinemabooking.services.MinioService;
import com.group10.cinemabooking.services.MovieService;
import com.group10.cinemabooking.utils.InAppCache;
import com.group10.cinemabooking.utils.LockManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private static final Logger log = LoggerFactory.getLogger(MovieServiceImpl.class);

    private final MovieRepository movieRepository;
    private final LockManager<String> lockManager;
    private final InAppCache<Long, Movies> movieCache;
    private final MinioService minioService;

    @Override
    @Transactional
    public ImgUrlDto createMovie(MovieDto movieDto) {
        Movies movie = new Movies();

        String lockKey = "movie:create:" + (movieDto != null && movieDto.getTitle() != null
                ? movieDto.getTitle().trim().toLowerCase()
                : "unknown");

        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();

        try {
            updateFromDto(movie, movieDto);
            saveMovie(movie);

            try {
                return minioService.uploadImage(movie.getMovie_id());
            } catch (Exception minioException) {
                log.warn(
                        "Movie created successfully but MinIO upload failed for movieId={}. Returning empty image URLs. Error: {}",
                        movie.getMovie_id(),
                        minioException.getMessage()
                );

                ImgUrlDto fallbackImgUrlDto = new ImgUrlDto();
                fallbackImgUrlDto.setHorizontal("");
                fallbackImgUrlDto.setVertical("");
                return fallbackImgUrlDto;
            }

        } catch (Exception e) {
            log.error("Error creating movie: {}", e.getMessage(), e);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<MovieDto> getAllMovies() {
        return movieRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public MovieDto getMovieById(Long movieId) {
        Movies movie = movieCache.getOrLoad(movieId, key -> {
            log.info("DB HIT for movieId={}", key);
            return movieRepository.findById(key)
                    .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + key));
        });

        return toDto(movie);
    }

    @Override
    @Transactional
    public MovieDto updateMovie(Long movieId, MovieDto movieDto) {
        Movies existingMovie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + movieId));

        ReentrantLock lock = lockManager.getLock("movie:update:" + movieId);
        lock.lock();

        try {
            updateFromDto(existingMovie, movieDto);
            saveMovie(existingMovie);
            return toDto(existingMovie);
        } catch (Exception e) {
            log.error("Error updating movie with id {}: {}", movieId, e.getMessage(), e);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public void deleteMovie(Long movieId) {
        Movies existingMovie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + movieId));

        ReentrantLock lock = lockManager.getLock("movie:delete:" + movieId);
        lock.lock();

        try {
            if (movieCache.contains(movieId)) {
                movieCache.remove(movieId);
            }
            movieRepository.delete(existingMovie);
        } catch (Exception e) {
            log.error("Error deleting movie with id {}: {}", movieId, e.getMessage(), e);
            throw e;
        } finally {
            lock.unlock();
        }
    }

    private void saveMovie(Movies movie) {
        movieRepository.save(movie);
        movieCache.put(movie.getMovie_id(), movie);
    }

    private MovieDto toDto(Movies movie) {
        if (movie == null) return null;

        MovieDto dto = new MovieDto();
        dto.setMovie_id(movie.getMovie_id());
        dto.setCreated_at(movie.getCreated_at());
        dto.setUpdated_at(movie.getUpdated_at());
        dto.setAge_rating(movie.getAge_rating());
        dto.setRelease_date(movie.getRelease_date());
        dto.setTitle(movie.getTitle());
        dto.setStatus(movie.getStatus());
        dto.setDescription(movie.getDescription());
        dto.setTrailerUrl(movie.getTrailerUrl());
        dto.setGenre(movie.getGenre());
        dto.setDuration_minutes(movie.getDuration_minutes());

        return dto;
    }

    private void updateFromDto(Movies movie, MovieDto dto) {
        if (dto.getAge_rating() != null) {
            movie.setAge_rating(dto.getAge_rating());
        }

        if (dto.getRelease_date() != null) {
            movie.setRelease_date(dto.getRelease_date());
        }

        if (dto.getTitle() != null) {
            movie.setTitle(dto.getTitle());
        }

        if (dto.getStatus() != null) {
            movie.setStatus(dto.getStatus());
        }

        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            movie.setDescription(dto.getDescription());
        }

        if (dto.getTrailerUrl() != null && !dto.getTrailerUrl().isBlank()) {
            movie.setTrailerUrl(dto.getTrailerUrl());
        }

        if (dto.getGenre() != null) {
            movie.setGenre(dto.getGenre());
        }

        if (dto.getDuration_minutes() > 0) {
            movie.setDuration_minutes(dto.getDuration_minutes());
        }
    }
}