package com.group10.cinemabooking.models;

import com.group10.cinemabooking.enums.ShowtimeSeatsStatusEnum;
import com.group10.cinemabooking.utils.IDGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShowTimeSeats {
    @Id
    @Builder.Default
    private long showtime_seat_id = IDGenerator.generateShowtimeSeatId();
    private Date hold_expires_at;
    private String hold_token;
    @NonNull
    @Column(nullable = false)
    private ShowtimeSeatsStatusEnum status;


    @ManyToOne(optional = false)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtimes showtime;

    @ManyToOne(optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seats seat;
}
