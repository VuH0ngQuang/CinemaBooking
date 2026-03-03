package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.dtos.UserDto;
import com.group10.cinemabooking.models.Users;
import com.group10.cinemabooking.repository.UserRepository;
import com.group10.cinemabooking.services.UserService;
import com.group10.cinemabooking.utils.InAppCache;
import com.group10.cinemabooking.utils.LockManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private final PasswordEncoder passwordEncoder;
    private final LockManager<Long> lockManager;
    private final InAppCache<Long, Users> userCache;
    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(LockManager<Long> lockManager,
                           InAppCache<Long, Users> userCache,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder
                           ) {
        this.passwordEncoder = passwordEncoder;
        this.lockManager = lockManager;
        this.userCache = userCache;
        this.userRepository = userRepository;
    }

    @Override
    public UserDto createUser(UserDto userDto) {
        Users user = new Users();
        ReentrantLock lock = lockManager.getLock(user.getUser_id());
        lock.lock();
        try {
            user.setEmail(userDto.getEmail());
            user.setFull_name(userDto.getFull_name());
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            saveUser(user);
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage());
        } finally {
            lock.unlock();
        }
        return userDto;
    }

    @Override
    public UserDto getUserById(Long id) {
        Users user = userCache.getOrLoad(id, key ->
                userRepository.findById(key).orElse(null)
        );
        if (user != null) {
            return toDto(user);
        }
        return null;
    }

    @Override
    public UserDto updateUser(Long id, UserDto userDto) {
        Users user = userCache.getOrLoad(id, key ->
                userRepository.findById(key).orElse(null)
        );
        if (user != null) {
            ReentrantLock lock = lockManager.getLock(id);
            lock.lock();
            try {
                if (userDto.getEmail() != null) user.setEmail(userDto.getEmail());
                if (userDto.getFull_name() != null) user.setFull_name(userDto.getFull_name());
                if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
                    user.setPassword(passwordEncoder.encode(userDto.getPassword()));
                }
                user.setUpdated_at(new Date());
                saveUser(user);
                return toDto(user);
            } catch (Exception e) {
                log.error("Error updating user with id {}: {}", id, e.getMessage());
            } finally {
                lock.unlock();
            }
        }
        return toDto(user);
    }

    @Override
    public void deleteUser(Long id) {
        ReentrantLock lock = lockManager.getLock(id);
        lock.lock();
        try {
            if(userCache.contains(id)) userCache.remove(id);
            userRepository.deleteById(id);
        } catch (Exception e) {
            log.error("Error deleting user with id {}: {}", id, e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Users getUserByEmail(String email) {
        try {
            return userRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            log.error("Error fetching user by email {}: {}", email, e.getMessage());
            return null;
        }
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Users user = getUserByEmail(email);
        return new User(
                user.getEmail(),
                user.getPassword(),
                List.of(() -> "ROLE_" + user.getRole().name())
        );
    }

    private void saveUser(Users user) {
        try {
            userRepository.save(user);
            userCache.put(user.getUser_id(),user);
        } catch (Exception e) {
            log.error("Error saving user with id {}: {}", user.getUser_id(), e.getMessage());
        }
    }

    private UserDto toDto(Users user) {
        if (user == null) return null;
        UserDto dto = new UserDto();
        dto.setUser_id(user.getUser_id());
        dto.setCreated_at(user.getCreated_at());
        dto.setUpdated_at(user.getUpdated_at());
        dto.setEmail(user.getEmail());
        dto.setFull_name(user.getFull_name());
        dto.setStatus(user.getStatus());
        dto.setRole(user.getRole());
        return dto;
    }
}
