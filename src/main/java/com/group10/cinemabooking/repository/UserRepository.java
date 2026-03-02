package com.group10.cinemabooking.repository;

import com.group10.cinemabooking.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Users, Long> {
}
