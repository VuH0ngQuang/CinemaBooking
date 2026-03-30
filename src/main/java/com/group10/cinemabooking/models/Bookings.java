package com.group10.cinemabooking.models;

import com.group10.cinemabooking.enums.BookingStatusEnum;
import com.group10.cinemabooking.utils.IDGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Bookings {

    @Id
    @Builder.Default
    private long booking_id = IDGenerator.generateBookingId();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatusEnum booking_status = BookingStatusEnum.PENDING;

    @NonNull
    @Column(nullable = false)
    private long total_price;

    private Date confirmed_at;

    @NonNull
    @Column(nullable = false)
    private Date expired_at;

    private Date updated_at;

    @Column(nullable = false)
    @Builder.Default
    private Date created_at = new Date();

    private Date canceled_at;

    @ManyToOne(optional = false)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtimes showtime;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tickets> tickets;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payments> payments;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingSeats> bookingSeats;
}