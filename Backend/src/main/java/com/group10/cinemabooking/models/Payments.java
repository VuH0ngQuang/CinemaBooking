package com.group10.cinemabooking.models;

import com.group10.cinemabooking.enums.PaymentStatusEnum;
import com.group10.cinemabooking.utils.IDGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.List;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payment_ref", columnList = "ref")
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payments {
    @Id
    @Builder.Default
    private long payment_id = IDGenerator.generatePaymentId();

    @Builder.Default
    @Column(nullable = false)
    private Date created_at = new Date();

    @Column(nullable = true)
    private Date updated_at;

    @NonNull
    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private PaymentStatusEnum status = PaymentStatusEnum.PENDING;

    @NonNull
    @Column(nullable = false)
    private String ref;

    @ToString.Exclude
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Bookings booking;

    @ToString.Exclude
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketValidations> ticketValidations;
}
