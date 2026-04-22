package com.group10.cinemabooking.models;

import com.group10.cinemabooking.enums.ShowtimeSeatsStatusEnum;
import com.group10.cinemabooking.utils.IDGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(
        name = "show_time_seats",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_showtime_seat", columnNames = {"showtime_id", "seat_id"})
        }
)
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
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ShowtimeSeatsStatusEnum status = ShowtimeSeatsStatusEnum.AVAILABLE;


    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtimes showtime;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seats seat;
}
