package com.group10.cinemabooking.models;

import com.group10.cinemabooking.utils.IDGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Seats {
    @Id
    @Builder.Default
    private long seat_id = IDGenerator.generateSeatId();
    @NonNull
    @Column(nullable = false)
    private int seat_row;
    @NonNull
    @Column(nullable = false)
    private int seat_col;
    @NonNull
    @Column(nullable = false)
    private char seat_label;
    @Builder.Default
    @Column(nullable = false)
    private boolean is_active = true;
    @NonNull
    @Column(nullable = false)
    private long seat_price;

    @ToString.Exclude
    @OneToMany(mappedBy = "seat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShowTimeSeats> showtimeSeats;
    @ToString.Exclude
    @OneToMany(mappedBy = "seat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingSeats> bookingSeats;
    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private ScreeningRooms screeningRoom;

}
