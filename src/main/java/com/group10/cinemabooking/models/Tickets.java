package com.group10.cinemabooking.models;

import com.group10.cinemabooking.enums.TicketStatusEnum;
import com.group10.cinemabooking.utils.IDGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ticket_code", columnNames = "ticket_code"),
                @UniqueConstraint(name = "uk_ticket_booking_seat", columnNames = {"booking_id", "seat_id"})
        }
)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Tickets {

    @Id
    @Builder.Default
    private long ticket_id = IDGenerator.generateTicketId();

    @NonNull
    @Column(nullable = false)
    private Date issued_at;

    @Column(nullable = true)
    private Date used_at;

    @NonNull
    @Column(nullable = false)
    private Date valid_until;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TicketStatusEnum status = TicketStatusEnum.VALID;

    @NonNull
    @Column(nullable = false, unique = true)
    private String ticket_code;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Bookings booking;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seats seat;
}