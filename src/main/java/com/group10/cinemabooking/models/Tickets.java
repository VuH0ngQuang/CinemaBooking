package com.group10.cinemabooking.models;

import com.group10.cinemabooking.enums.TicketStatusEnum;
import com.group10.cinemabooking.utils.IDGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.*;

import java.util.Date;

@Entity
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
    @NonNull
    @Column(nullable = false)
    private Date used_at;
    @NonNull
    @Column(nullable = false)
    private Date valid_until;
    @Column(nullable = false)
    @Builder.Default
    private TicketStatusEnum status = TicketStatusEnum.VALID;
    @NonNull
    @Column(nullable = false)
    private String ticket_code;

    @ManyToOne(optional = false)
    private Bookings booking;
}
