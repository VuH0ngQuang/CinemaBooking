package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.dtos.ShowtimeDto;
import com.group10.cinemabooking.exception.InvalidRequestException;
import com.group10.cinemabooking.exception.ResourceNotFoundException;
import com.group10.cinemabooking.models.Movies;
import com.group10.cinemabooking.models.ScreeningRooms;
import com.group10.cinemabooking.models.Showtimes;
import com.group10.cinemabooking.repository.ShowtimeRepository;
import com.group10.cinemabooking.services.ShowtimeService;
import com.group10.cinemabooking.utils.InAppCache;
import com.group10.cinemabooking.utils.LockManager;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ShowtimeServiceImpl implements ShowtimeService {

    private static final Logger log = LoggerFactory.getLogger(ShowtimeServiceImpl.class);

    private final ShowtimeRepository showtimeRepository;
    private final LockManager<Long> lockManager;
    private final InAppCache<Long, Showtimes> showtimeCache;
    private final EntityManager entityManager;

    public ShowtimeServiceImpl(ShowtimeRepository showtimeRepository,
                               LockManager<Long> lockManager,
                               InAppCache<Long, Showtimes> showtimeCache,
                               EntityManager entityManager) {
        this.showtimeRepository = showtimeRepository;
        this.lockManager = lockManager;
        this.showtimeCache = showtimeCache;
        this.entityManager = entityManager;
    }

    @Override
    public ShowtimeDto createShowtime(ShowtimeDto showtimeDto) {
        validateTime(showtimeDto);

        Showtimes showtime = new Showtimes();
        ReentrantLock lock = lockManager.getLock(showtime.getShowtime_id());
        lock.lock();
        try {
            updateFromDto(showtime, showtimeDto);
            saveShowtime(showtime);
            return toDto(showtime);
        } catch (Exception e) {
            log.error("Error creating showtime: {}", e.getMessage());
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<ShowtimeDto> getAllShowtimes() {
        return showtimeRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public ShowtimeDto getShowtimeById(Long showtimeId) {
        Showtimes showtime = showtimeCache.getOrLoad(showtimeId, key ->
                showtimeRepository.findById(key)
                        .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + key))
        );
        return toDto(showtime);
    }

    @Override
    public ShowtimeDto updateShowtime(Long showtimeId, ShowtimeDto showtimeDto) {
        Showtimes existingShowtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + showtimeId));

        validateTime(showtimeDto);

        ReentrantLock lock = lockManager.getLock(showtimeId);
        lock.lock();
        try {
            updateFromDto(existingShowtime, showtimeDto);
            saveShowtime(existingShowtime);
            return toDto(existingShowtime);
        } catch (Exception e) {
            log.error("Error updating showtime with id {}: {}", showtimeId, e.getMessage());
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void deleteShowtime(Long showtimeId) {
        Showtimes existingShowtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found with id: " + showtimeId));

        ReentrantLock lock = lockManager.getLock(showtimeId);
        lock.lock();
        try {
            if (showtimeCache.contains(showtimeId)) {
                showtimeCache.remove(showtimeId);
            }
            showtimeRepository.delete(existingShowtime);
        } catch (Exception e) {
            log.error("Error deleting showtime with id {}: {}", showtimeId, e.getMessage());
            throw e;
        } finally {
            lock.unlock();
        }
    }

    private void validateTime(ShowtimeDto dto) {
        if (dto.getStart_time() != null && dto.getEnd_time() != null
                && !dto.getEnd_time().after(dto.getStart_time())) {
            throw new InvalidRequestException("End time must be after start time");
        }
    }

    private void saveShowtime(Showtimes showtime) {
        showtimeRepository.save(showtime);
        showtimeCache.put(showtime.getShowtime_id(), showtime);
    }

    private ShowtimeDto toDto(Showtimes showtime) {
        if (showtime == null) return null;

        ShowtimeDto dto = new ShowtimeDto();
        dto.setShowtime_id(showtime.getShowtime_id());
        dto.setStatus(showtime.getStatus());
        dto.setStart_time(showtime.getStart_time());
        dto.setEnd_time(showtime.getEnd_time());
        dto.setCreated_at(showtime.getCreated_at());
        dto.setUpdated_at(showtime.getUpdated_at());
        dto.setBuffer_time(showtime.getBuffer_time());
        dto.setMovie_id(showtime.getMovie() != null ? showtime.getMovie().getMovie_id() : null);
        dto.setScreening_room_id(showtime.getScreeningRoom() != null ? showtime.getScreeningRoom().getRoom_id() : null);

        return dto;
    }

    private void updateFromDto(Showtimes showtime, ShowtimeDto dto) {
        if (dto.getStatus() != null) {
            showtime.setStatus(dto.getStatus());
        }
        if (dto.getStart_time() != null) {
            showtime.setStart_time(dto.getStart_time());
        }
        if (dto.getEnd_time() != null) {
            showtime.setEnd_time(dto.getEnd_time());
        }

        showtime.setBuffer_time(dto.getBuffer_time());

        if (dto.getMovie_id() != null) {
            Movies movieRef = entityManager.getReference(Movies.class, dto.getMovie_id());
            showtime.setMovie(movieRef);
        }

        if (dto.getScreening_room_id() != null) {
            ScreeningRooms roomRef = entityManager.getReference(ScreeningRooms.class, dto.getScreening_room_id());
            showtime.setScreeningRoom(roomRef);
        }
    }
}