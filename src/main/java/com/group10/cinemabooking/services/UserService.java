package com.group10.cinemabooking.services;

import com.group10.cinemabooking.dtos.UserDto;
import com.group10.cinemabooking.models.Users;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;


public interface UserService extends UserDetailsService {
    UserDto createUser(UserDto userDto);
    Users getUserById(Long id);
    UserDto updateUser(Long id, UserDto userDto);
    Users getUserByEmail(String email);
    void deleteUser(Long id);
    UserDto restoreUser(Long id);
    List<UserDto> getAllUsers();
    UserDto getUserDtoById(Long id);
    UserDto getUserDtoByEmail(String email);
}
