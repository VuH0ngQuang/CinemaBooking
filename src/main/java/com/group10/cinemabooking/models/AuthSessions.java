package com.group10.cinemabooking.models;

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
public class AuthSessions {
    @Id
    @Builder.Default
    private long session_id = IDGenerator.generateAuthSessionId();

    @NonNull
    @Column(nullable = false)
    private Date expires_at;

    @NonNull
    @Column(nullable = false)
    private Date created_at;

    @NonNull
    @Column(nullable = false)
    private String session_token;

    @Column(nullable = true)
    private Date revoked_at;

    @ToString.Exclude
    @ManyToOne(optional = false)
    private Users user;
}
