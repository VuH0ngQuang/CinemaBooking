package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.dtos.CinemaDto;
import com.group10.cinemabooking.dtos.CinemaRequestDto;
import com.group10.cinemabooking.models.Cinemas;
import com.group10.cinemabooking.exception.ResourceNotFoundException;
import com.group10.cinemabooking.repository.CinemaRepository;
import com.group10.cinemabooking.services.CinemaService;
import com.group10.cinemabooking.utils.InAppCache;
import com.group10.cinemabooking.utils.LockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;
    private final InAppCache<Long, Cinemas> cinemaCache;
    private final LockManager lockManager;

    private CinemaDto toDto(Cinemas cinema) {
        return CinemaDto.builder()
                .cinemas_id(cinema.getCinemas_id())
                .name(cinema.getName())
                .address(cinema.getAddress())
                .created_at(cinema.getCreated_at())
                .build();
    }

    private void updateFromDto(CinemaRequestDto requestDto, Cinemas cinema) {
        if (requestDto.getName() != null && !requestDto.getName().trim().isEmpty()) {
            cinema.setName(requestDto.getName().trim());
        }

        if (requestDto.getAddress() != null && !requestDto.getAddress().trim().isEmpty()) {
            cinema.setAddress(requestDto.getAddress().trim());
        }
    }

    @Override
    @Transactional
    public CinemaDto createCinema(CinemaRequestDto requestDto) {
        ReentrantLock lock = lockManager.getLock("cinema_create");
        lock.lock();
        try {
            Cinemas cinema = Cinemas.builder()
                    .name(requestDto.getName() != null ? requestDto.getName().trim() : null)
                    .address(requestDto.getAddress() != null ? requestDto.getAddress().trim() : null)
                    .build();

            Cinemas savedCinema = cinemaRepository.save(cinema);
            cinemaCache.put(savedCinema.getCinemas_id(), savedCinema);

            return toDto(savedCinema);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<CinemaDto> getAllCinemas() {
        return cinemaRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public CinemaDto getCinemaById(Long cinemaId) {
        Cinemas cinema = cinemaCache.getOrLoad(cinemaId, key ->
                cinemaRepository.findById(key)
                        .orElseThrow(() -> new ResourceNotFoundException("Cinema not found with id: " + key))
        );

        return toDto(cinema);
    }

    @Override
    @Transactional
    public CinemaDto updateCinema(Long cinemaId, CinemaRequestDto requestDto) {
        ReentrantLock lock = lockManager.getLock(cinemaId);
        lock.lock();
        try {
            Cinemas cinema = cinemaRepository.findById(cinemaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cinema not found with id: " + cinemaId));

            updateFromDto(requestDto, cinema);

            Cinemas updatedCinema = cinemaRepository.save(cinema);
            cinemaCache.put(cinemaId, updatedCinema);

            return toDto(updatedCinema);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public void deleteCinema(Long cinemaId) {
        ReentrantLock lock = lockManager.getLock(cinemaId);
        lock.lock();
        try {
            Cinemas cinema = cinemaRepository.findById(cinemaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cinema not found with id: " + cinemaId));

            cinemaRepository.delete(cinema);
            cinemaCache.remove(cinemaId);
        } finally {
            lock.unlock();
        }
    }
}