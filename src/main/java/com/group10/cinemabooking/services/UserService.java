package com.group10.cinemabooking.services;

import com.group10.cinemabooking.dtos.UserDto;
import com.group10.cinemabooking.models.Users;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collection;

public interface UserService extends UserDetailsService {
    UserDto createUser(UserDto userDto);
    UserDto getUserById(Long id);
    UserDto updateUser(Long id, UserDto userDto);
    Users getUserByEmail(String email);
    void deleteUser(Long id);
}
