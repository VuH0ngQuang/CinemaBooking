package com.group10.cinemabooking.models;

import com.group10.cinemabooking.enums.UserRoleEnum;
import com.group10.cinemabooking.enums.UserStatusEnum;
import com.group10.cinemabooking.utils.IDGenerator;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.List;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Users {
    @Id
    @Builder.Default
    private long user_id = IDGenerator.generateUserId();
    @NonNull
    @Column(nullable = false)
    private String email;
    @Builder.Default
    @Column(nullable = false)
    private Date created_at = new Date();
    private Date updated_at;
    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatusEnum status = UserStatusEnum.ACTIVE;
    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRoleEnum role = UserRoleEnum.CUSTOMER;
    @NonNull
    @Column(nullable = false)
    private String full_name;
    @NonNull
    @Column(nullable = false)
    private String password;

    @ToString.Exclude
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bookings> bookings;
    @ToString.Exclude
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AuthSessions> authSessions;
}
