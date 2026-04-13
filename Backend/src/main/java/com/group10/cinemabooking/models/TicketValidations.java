package com.group10.cinemabooking.models;

import com.group10.cinemabooking.enums.TicketResultEnum;
import com.group10.cinemabooking.utils.IDGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Date;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TicketValidations {
    @Id
    @Builder.Default
    private long validation_id = IDGenerator.generateTicketValidationId();
    @NonNull
    @Column(nullable = false)
    private Date validated_at;

    @Enumerated(EnumType.STRING)
    private TicketResultEnum result;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Payments payment;
}
