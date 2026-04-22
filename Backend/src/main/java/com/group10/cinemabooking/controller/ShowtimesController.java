package com.group10.cinemabooking.controller;

import com.group10.cinemabooking.dtos.ShowtimeDto;
import com.group10.cinemabooking.dtos.ShowtimeSeatStatusDto;
import com.group10.cinemabooking.services.ShowtimeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/showtimes")
public class ShowtimesController {

    private final ShowtimeService showtimeService;

    public ShowtimesController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    @PostMapping
    public ResponseEntity<ShowtimeDto> createShowtime(@Valid @RequestBody ShowtimeDto showtimeDto) {
        ShowtimeDto createdShowtime = showtimeService.createShowtime(showtimeDto);
        return new ResponseEntity<>(createdShowtime, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ShowtimeDto>> getAllShowtimes() {
        List<ShowtimeDto> showtimes = showtimeService.getAllShowtimes();
        return ResponseEntity.ok(showtimes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowtimeDto> getShowtimeById(@PathVariable("id") Long showtimeId) {
        ShowtimeDto showtime = showtimeService.getShowtimeById(showtimeId);
        return ResponseEntity.ok(showtime);
    }

    @GetMapping("/movie/{id}")
    public ResponseEntity<List<ShowtimeDto>> getAllByMovieId(@PathVariable("id") Long movieId) {
        List<ShowtimeDto> showtimes = showtimeService.getAllByMovieId(movieId);
        return ResponseEntity.ok(showtimes);
    }

    @GetMapping("/{id}/seats/status")
    public ResponseEntity<List<ShowtimeSeatStatusDto>> getSeatStatusesByShowtime(
            @PathVariable("id") Long showtimeId,
            @RequestParam(value = "bookingId", required = false) Long bookingId
    ) {
        return ResponseEntity.ok(showtimeService.getSeatStatusesByShowtimeId(showtimeId, bookingId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShowtimeDto> updateShowtime(@PathVariable("id") Long showtimeId,
                                                      @Valid @RequestBody ShowtimeDto showtimeDto) {
        ShowtimeDto updatedShowtime = showtimeService.updateShowtime(showtimeId, showtimeDto);
        return ResponseEntity.ok(updatedShowtime);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteShowtime(@PathVariable("id") Long showtimeId) {
        showtimeService.deleteShowtime(showtimeId);
        return ResponseEntity.ok("Showtime deleted successfully with id: " + showtimeId);
    }
}