package com.group10.cinemabooking.models;

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

    @ManyToOne(optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Bookings booking;

    @ManyToOne(optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seats seat;

    @Column(nullable = false)
    private double price;
}