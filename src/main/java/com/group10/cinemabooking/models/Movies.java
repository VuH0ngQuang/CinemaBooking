package com.group10.cinemabooking.models;

import com.group10.cinemabooking.enums.AgeRatingEnum;
import com.group10.cinemabooking.enums.MovieGenreEnum;
import com.group10.cinemabooking.enums.MovieStatusEnum;
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
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class Movies {
    @Id
    @Builder.Default
    private long movie_id = IDGenerator.generateMovieId();
    @Builder.Default
    @Column(nullable = false)
    private Date created_at = new Date();
    private Date updated_at;
    @NonNull
    @Column(nullable = false)
    private AgeRatingEnum age_rating;
    @NonNull
    @Column(nullable = false)
    private Date release_date;
    @NonNull
    @Column(nullable = false)
    private String title;
    @NonNull
    @Column(nullable = false)
    private MovieStatusEnum status;
    @NonNull
    @Column(nullable = false)
    private String description;
    @NonNull
    @Column(nullable = false)
    private MovieGenreEnum genre;
    @NonNull
    @Column(nullable = false)
    private int duration_minutes;

    @OneToMany(mappedBy = "movie", orphanRemoval = true)
    private List<Showtimes> showtimesList;
}
