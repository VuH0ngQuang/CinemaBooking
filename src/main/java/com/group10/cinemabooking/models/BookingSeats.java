package com.group10.cinemabooking.models;

import com.group10.cinemabooking.enums.BookingSeatStatusEnum;
import com.group10.cinemabooking.utils.IDGenerator;
import jakarta.persistence.*;
import lombok.*;

@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BookingSeats {

    @Id
    @Builder.Default
    private long booking_seat_id = IDGenerator.generateBookingSeatId();

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Bookings booking;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seats seat;

    @Column(nullable = false)
    private Long price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingSeatStatusEnum status = BookingSeatStatusEnum.LOCKED;
}