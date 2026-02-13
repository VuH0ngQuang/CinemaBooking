package com.group10.cinemabooking.models;

import com.group10.cinemabooking.utils.IDGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.util.Date;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cinemas {
    @Id
    @Builder.Default
    private long cinemas_id = IDGenerator.generateCinemaId();
    @NonNull
    @Column(nullable = false)
    private String name;
    @NonNull
    @Column(nullable = false)
    private String address;
    @Builder.Default
    @Column(nullable = false)
    private Date created_at = new Date();

    @OneToMany(mappedBy = "cinema")
    private List<ScreeningRooms> screeningRooms;
}
