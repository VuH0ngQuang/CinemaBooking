package com.group10.cinemabooking.services;

import com.group10.cinemabooking.dtos.UserDto;
import com.group10.cinemabooking.models.Users;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.data.domain.Page;
import java.util.Collection;
import java.util.List;

public interface UserService extends UserDetailsService {
    List<UserDto> getAllUsers();
    Page<UserDto> getAllUsersPaginated(int page, int size);
    boolean changePassword(Long id, String oldPassword, String newPassword);
    UserDto createUser(UserDto userDto);
    UserDto getUserById(Long id);
    UserDto updateUser(Long id, UserDto userDto);
    Users getUserByEmail(String email);
    void deleteUser(Long id);
    void softDeleteUser(Long id);
    
}
