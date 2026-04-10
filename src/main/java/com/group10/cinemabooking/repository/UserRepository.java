package com.group10.cinemabooking.repository;

import com.group10.cinemabooking.models.Users;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u.user_id FROM Users u WHERE u.email = :email AND u.is_deleted = false")
    Optional<Long> getUserIdByEmail(@Param("email") String email);

    @Query("SELECT u FROM Users u WHERE u.user_id = :id AND u.is_deleted = false")
    Optional<Users> findActiveById(@Param("id") Long id);

    @Query("SELECT u FROM Users u WHERE u.email = :email AND u.is_deleted = false")
    Optional<Users> findActiveByEmail(@Param("email") String email);

    @Query("SELECT u FROM Users u WHERE u.is_deleted = false")
    List<Users> findAllActive();
}
