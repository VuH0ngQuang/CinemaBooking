package com.group10.cinemabooking.models;

import com.group10.cinemabooking.enums.ShowtimeStatusEnum;
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
public class Showtimes {
    @Id
    @Builder.Default
    private long showtime_id = IDGenerator.generateShowtimeId();
    @Column(nullable = false)
    @Builder.Default
    private ShowtimeStatusEnum status = ShowtimeStatusEnum.SCHEDULED;
    @NonNull
    @Column(nullable = false)
    private Date start_time;
    @NonNull
    @Column(nullable = false)
    private Date end_time;
    @NonNull
    @Builder.Default
    @Column(nullable = false)
    private Date created_at = new Date();
    private Date updated_at;
    @NonNull
    @Column(nullable = false)
    private int buffer_time;

    @ManyToOne(optional = false)
    private ScreeningRooms screeningRoom;
    @ManyToOne(optional = false)
    private Movies movie;
    @OneToMany(mappedBy = "showtime", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShowTimeSeats> showtimeSeatList;
    @OneToMany(mappedBy = "showtime", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bookings> bookingsList;
}
