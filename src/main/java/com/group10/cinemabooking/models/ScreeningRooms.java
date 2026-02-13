package com.group10.cinemabooking.models;

import com.group10.cinemabooking.utils.IDGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ScreeningRooms {
    @Id
    @Builder.Default
    private long room_id = IDGenerator.generateScreeningRoomId();
    @NonNull
    @Column(nullable = false)
    private String room_name;
    @NonNull
    @Column(nullable = false)
    private int amount_rows;
    @NonNull
    @Column(nullable = false)
    private int amount_cols;

    @ManyToOne
    private Cinemas cinema;
    @OneToMany(mappedBy = "screeningRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Showtimes> showtime;
    @OneToMany(mappedBy = "screeningRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seats> seat;
}
