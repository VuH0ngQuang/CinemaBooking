package com.group10.cinemabooking.controller;

import com.group10.cinemabooking.dtos.CinemaDto;
import com.group10.cinemabooking.dtos.CinemaRequestDto;
import com.group10.cinemabooking.services.CinemaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cinemas")
@RequiredArgsConstructor
public class CinemaController {

    private final CinemaService cinemaService;

    @PostMapping
    public ResponseEntity<CinemaDto> createCinema(@Valid @RequestBody CinemaRequestDto requestDto) {
        CinemaDto createdCinema = cinemaService.createCinema(requestDto);
        return new ResponseEntity<>(createdCinema, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CinemaDto>> getAllCinemas() {
        List<CinemaDto> cinemas = cinemaService.getAllCinemas();
        return ResponseEntity.ok(cinemas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CinemaDto> getCinemaById(@PathVariable("id") Long cinemaId) {
        CinemaDto cinema = cinemaService.getCinemaById(cinemaId);
        return ResponseEntity.ok(cinema);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CinemaDto> updateCinema(@PathVariable("id") Long cinemaId,
                                                  @Valid @RequestBody CinemaRequestDto requestDto) {
        CinemaDto updatedCinema = cinemaService.updateCinema(cinemaId, requestDto);
        return ResponseEntity.ok(updatedCinema);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCinema(@PathVariable("id") Long cinemaId) {
        cinemaService.deleteCinema(cinemaId);
        return ResponseEntity.noContent().build();
    }
}