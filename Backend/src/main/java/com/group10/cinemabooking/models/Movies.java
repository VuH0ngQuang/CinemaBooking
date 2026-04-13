package com.group10.cinemabooking.models;

import com.group10.cinemabooking.enums.AgeRatingEnum;
import com.group10.cinemabooking.enums.MovieGenreEnum;
import com.group10.cinemabooking.enums.MovieStatusEnum;
import com.group10.cinemabooking.utils.IDGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "movies")
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
    @Temporal(TemporalType.TIMESTAMP)
    private Date created_at = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    private Date updated_at;

    @NotNull(message = "Age rating must not be null")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AgeRatingEnum age_rating;

    @NotNull(message = "Release date must not be null")
    @Column(nullable = false)
    @Temporal(TemporalType.DATE)
    private Date release_date;

    @NotNull(message = "Title must not be null")
    @Column(nullable = false)
    private String title;

    @NotNull(message = "Status must not be null")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MovieStatusEnum status;

    @NotBlank(message = "Description must not be null")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Genre must not be null")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MovieGenreEnum genre;

    @Min(value = 1, message = "Duration must be greater than 0")
    @Column(nullable = false)
    private int duration_minutes;

    @ToString.Exclude
    @OneToMany(mappedBy = "movie", orphanRemoval = true)
    private List<Showtimes> showtimesList;

    @PrePersist
    protected void onCreate() {
        if (this.created_at == null) {
            this.created_at = new Date();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updated_at = new Date();
    }
}