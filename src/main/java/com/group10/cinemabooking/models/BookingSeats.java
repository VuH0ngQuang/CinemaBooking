package com.group10.cinemabooking.models;

import com.group10.cinemabooking.utils.IDGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class BookingSeats {
    @Id
    @Builder.Default
    private long booking_seat_id = IDGenerator.generateBookingSeatId();
    @NonNull
    @Column(nullable = false)
    private long unit_price;
    @Builder.Default
    @Column(nullable = false)
    private Date created_at = new Date();

    @ManyToOne(optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Bookings booking;
    @ManyToOne(optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seats seat;
}
