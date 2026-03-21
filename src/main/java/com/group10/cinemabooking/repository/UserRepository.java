package com.group10.cinemabooking.repository;

import com.group10.cinemabooking.models.Users;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u.user_id FROM Users u WHERE u.email = :email")
    Optional<Long> getUserIdByEmail(@Param("email") String email);

}
