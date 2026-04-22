package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.dtos.ScreeningRoomDto;
import com.group10.cinemabooking.exception.InvalidRequestException;
import com.group10.cinemabooking.exception.ResourceNotFoundException;
import com.group10.cinemabooking.models.Cinemas;
import com.group10.cinemabooking.models.ScreeningRooms;
import com.group10.cinemabooking.models.Seats;
import com.group10.cinemabooking.repository.ScreeningRoomRepository;
import com.group10.cinemabooking.repository.SeatRepository;
import com.group10.cinemabooking.services.ScreeningRoomService;
import com.group10.cinemabooking.utils.InAppCache;
import com.group10.cinemabooking.utils.LockManager;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScreeningRoomServiceImpl implements ScreeningRoomService {

    private static final Logger log = LoggerFactory.getLogger(ScreeningRoomServiceImpl.class);

    private final ScreeningRoomRepository screeningRoomRepository;
    private final LockManager<String> lockManager;
    private final InAppCache<Long, ScreeningRooms> screeningRoomCache;
    private final EntityManager entityManager;
    private final SeatRepository seatRepository;


    @Override
    @Transactional
    public ScreeningRoomDto createScreeningRoom(ScreeningRoomDto screeningRoomDto) {
        if (screeningRoomDto == null) {
            throw new InvalidRequestException("Screening room payload must not be null");
        }
        ScreeningRooms screeningRoom = new ScreeningRooms();
        String roomName = screeningRoomDto != null && screeningRoomDto.getRoom_name() != null
                ? screeningRoomDto.getRoom_name().trim().toLowerCase()
                : "unknown";
        String lockKey = "screeningRoom:create:" + screeningRoomDto.getCinema_id() + ":" + roomName;
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            updateFromDto(screeningRoom, screeningRoomDto);
            saveScreeningRoom(screeningRoom);

            int rows = screeningRoom.getAmount_rows();
            int cols = screeningRoom.getAmount_cols();
            if (rows <= 0 || cols <= 0) {
                throw new InvalidRequestException("Room rows/cols must be > 0");
            }
            for (int r = 1; r <= rows; r++) {
                if (r > 26) {
                    throw new InvalidRequestException("Current seat_label mapping supports up to 26 rows");
                }
                char seatLabel = (char) ('A' + r - 1);
                for (int c = 1; c <= cols; c++) {
                    Seats seat = Seats.builder()
                            .seat_row(r)
                            .seat_col(c)
                            .seat_label(seatLabel)
                            .is_active(true)
                            .screeningRoom(screeningRoom)
                            .build();
                    seatRepository.save(seat);
                }
            }

            return toDto(screeningRoom);
        } catch (Exception e) {
            log.error("Error creating screening room: {}", e.getMessage());
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<ScreeningRoomDto> getAllScreeningRooms() {
        return screeningRoomRepository.findAllJoinFetch()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public ScreeningRoomDto getScreeningRoomById(Long roomId) {
        ScreeningRooms screeningRoom = screeningRoomCache.getOrLoad(roomId, key ->
                screeningRoomRepository.findById(key)
                        .orElseThrow(() -> new ResourceNotFoundException("Screening room not found with id: " + key))
        );

        return toDto(screeningRoom);
    }

    @Override
    @Transactional
    public ScreeningRoomDto updateScreeningRoom(Long roomId, ScreeningRoomDto screeningRoomDto) {
        ScreeningRooms existingScreeningRoom = screeningRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Screening room not found with id: " + roomId));

        ReentrantLock lock = lockManager.getLock("screeningRoom:update:" + roomId);
        lock.lock();
        try {
            updateFromDto(existingScreeningRoom, screeningRoomDto);
            saveScreeningRoom(existingScreeningRoom);
            return toDto(existingScreeningRoom);
        } catch (Exception e) {
            log.error("Error updating screening room with id {}: {}", roomId, e.getMessage());
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public void deleteScreeningRoom(Long roomId) {
        ScreeningRooms existingScreeningRoom = screeningRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Screening room not found with id: " + roomId));

        ReentrantLock lock = lockManager.getLock("screeningRoom:delete:" + roomId);
        lock.lock();
        try {
            if (screeningRoomCache.contains(roomId)) {
                screeningRoomCache.remove(roomId);
            }
            screeningRoomRepository.delete(existingScreeningRoom);
        } catch (Exception e) {
            log.error("Error deleting screening room with id {}: {}", roomId, e.getMessage());
            throw e;
        } finally {
            lock.unlock();
        }
    }

    private void saveScreeningRoom(ScreeningRooms screeningRoom) {
        screeningRoomRepository.save(screeningRoom);
        screeningRoomCache.put(screeningRoom.getRoom_id(), screeningRoom);
    }

    private ScreeningRoomDto toDto(ScreeningRooms screeningRoom) {
        if (screeningRoom == null) return null;

        ScreeningRoomDto dto = new ScreeningRoomDto();
        dto.setRoom_id(screeningRoom.getRoom_id());
        dto.setRoom_name(screeningRoom.getRoom_name());
        dto.setAmount_rows(screeningRoom.getAmount_rows());
        dto.setAmount_cols(screeningRoom.getAmount_cols());

        if (screeningRoom.getCinema() != null) {
            dto.setCinema_id(screeningRoom.getCinema().getCinemas_id());
        }

        return dto;
    }

    private void updateFromDto(ScreeningRooms screeningRoom, ScreeningRoomDto dto) {
        if (dto.getRoom_name() != null && !dto.getRoom_name().isBlank()) {
            screeningRoom.setRoom_name(dto.getRoom_name());
        }

        if (dto.getAmount_rows() > 0) {
            screeningRoom.setAmount_rows(dto.getAmount_rows());
        }

        if (dto.getAmount_cols() > 0) {
            screeningRoom.setAmount_cols(dto.getAmount_cols());
        }

        if (dto.getCinema_id() != null) {
            Cinemas cinemaRef = entityManager.getReference(Cinemas.class, dto.getCinema_id());
            screeningRoom.setCinema(cinemaRef);
        }
    }
}