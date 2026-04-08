package com.group10.cinemabooking.models;

import com.group10.cinemabooking.enums.ShowtimeStatusEnum;
import com.group10.cinemabooking.utils.IDGenerator;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

import java.util.Date;
import java.util.List;

@Entity
@Table(name = "showtimes")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Showtimes {

    @Id
    @Builder.Default
    private long showtime_id = IDGenerator.generateShowtimeId();

    @NotNull(message = "Showtime status must not be null")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ShowtimeStatusEnum status = ShowtimeStatusEnum.SCHEDULED;

    @NotNull(message = "Start time must not be null")
    @NonNull
    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date start_time;

    @NotNull(message = "End time must not be null")
    @NonNull
    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date end_time;

    @NotNull(message = "Seat price must not be null")
    @NonNull
    @Column(nullable = false)
    private Long seat_price;

    @NonNull
    @Builder.Default
    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date created_at = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    private Date updated_at;

    @Min(value = 0, message = "Buffer time must be greater than or equal to 0")
    @NonNull
    @Column(nullable = false)
    private int buffer_time;

    @NotNull(message = "Screening room must not be null")
    @ToString.Exclude
    @ManyToOne(optional = false)
    private ScreeningRooms screeningRoom;

    @NotNull(message = "Movie must not be null")
    @ToString.Exclude
    @ManyToOne(optional = false)
    private Movies movie;

    @ToString.Exclude
    @OneToMany(mappedBy = "showtime", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShowTimeSeats> showtimeSeatList;

    @ToString.Exclude
    @OneToMany(mappedBy = "showtime", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bookings> bookingsList;

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