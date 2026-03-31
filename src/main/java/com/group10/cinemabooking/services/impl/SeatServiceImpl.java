package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.dtos.SeatDto;
import com.group10.cinemabooking.dtos.SeatRequestDto;
import com.group10.cinemabooking.exception.InvalidRequestException;
import com.group10.cinemabooking.exception.ResourceNotFoundException;
import com.group10.cinemabooking.models.ScreeningRooms;
import com.group10.cinemabooking.models.Seats;
import com.group10.cinemabooking.repository.ScreeningRoomRepository;
import com.group10.cinemabooking.repository.SeatRepository;
import com.group10.cinemabooking.services.SeatService;
import com.group10.cinemabooking.utils.InAppCache;
import com.group10.cinemabooking.utils.LockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final ScreeningRoomRepository screeningRoomRepository;
    private final LockManager<String> lockManager;
    private final InAppCache<Long, Seats> seatCache;

    @Override
    @Transactional
    public SeatDto createSeat(SeatRequestDto requestDto) {
        String lockKey = "seat:create:" + requestDto.getRoom_id() + ":" + requestDto.getSeat_row() + ":" + requestDto.getSeat_col();
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            ScreeningRooms screeningRoom = screeningRoomRepository.findById(requestDto.getRoom_id())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Screening room not found with id: " + requestDto.getRoom_id()
                    ));

            boolean existed = seatRepository.existsByRoomAndPosition(
                    requestDto.getRoom_id(),
                    requestDto.getSeat_row(),
                    requestDto.getSeat_col()
            );

            if (existed) {
                throw new InvalidRequestException("Seat already exists in this screening room.");
            }

            Seats seat = Seats.builder()
                    .seat_row(requestDto.getSeat_row())
                    .seat_col(requestDto.getSeat_col())
                    .seat_label(requestDto.getSeat_label())
                    .is_active(requestDto.is_active())
                    .screeningRoom(screeningRoom)
                    .build();

            Seats savedSeat = seatRepository.save(seat);
            seatCache.put(savedSeat.getSeat_id(), savedSeat);

            return toDto(savedSeat);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<SeatDto> getAllSeats() {
        return seatRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public SeatDto getSeatById(Long seatId) {
        Seats seat = seatCache.getOrLoad(seatId, key ->
                seatRepository.findById(key)
                        .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + key))
        );

        return toDto(seat);
    }

    @Override
    @Transactional
    public SeatDto updateSeat(Long seatId, SeatRequestDto requestDto) {
        String lockKey = "seat:update:" + seatId;
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            Seats existingSeat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + seatId));

            ScreeningRooms screeningRoom = screeningRoomRepository.findById(requestDto.getRoom_id())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Screening room not found with id: " + requestDto.getRoom_id()
                    ));

            boolean duplicated = seatRepository.existsByRoomAndPosition(
                    requestDto.getRoom_id(),
                    requestDto.getSeat_row(),
                    requestDto.getSeat_col()
            );

            boolean changedPosition =
                    !Objects.equals(existingSeat.getScreeningRoom().getRoom_id(), requestDto.getRoom_id())
                        || existingSeat.getSeat_row() != requestDto.getSeat_row()
                        || existingSeat.getSeat_col() != requestDto.getSeat_col();

            if (changedPosition && duplicated) {
                throw new InvalidRequestException("Seat already exists in this screening room.");
            }

            updateFromDto(existingSeat, requestDto, screeningRoom);

            Seats updatedSeat = seatRepository.save(existingSeat);
            seatCache.put(updatedSeat.getSeat_id(), updatedSeat);

            return toDto(updatedSeat);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public void deleteSeat(Long seatId) {
        String lockKey = "seat:delete:" + seatId;
        ReentrantLock lock = lockManager.getLock(lockKey);
        lock.lock();
        try {
            Seats seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + seatId));

            seatRepository.delete(seat);
            seatCache.remove(seatId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<SeatDto> getSeatsByRoomId(Long roomId) {
        return seatRepository.findByRoomId(roomId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private SeatDto toDto(Seats seat) {
        return SeatDto.builder()
                .seat_id(seat.getSeat_id())
                .seat_row(seat.getSeat_row())
                .seat_col(seat.getSeat_col())
                .seat_label(seat.getSeat_label())
                .is_active(seat.is_active())
                .room_id(seat.getScreeningRoom() != null ? seat.getScreeningRoom().getRoom_id() : null)
                .build();
    }

    private void updateFromDto(Seats seat, SeatRequestDto requestDto, ScreeningRooms screeningRoom) {
        seat.setSeat_row(requestDto.getSeat_row());
        seat.setSeat_col(requestDto.getSeat_col());
        seat.setSeat_label(requestDto.getSeat_label());
        seat.set_active(requestDto.is_active());
        seat.setScreeningRoom(screeningRoom);
    }
}