package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.dtos.UserDto;
import com.group10.cinemabooking.exception.InvalidRequestException;
import com.group10.cinemabooking.exception.ResourceNotFoundException;
import com.group10.cinemabooking.models.Users;
import com.group10.cinemabooking.repository.UserRepository;
import com.group10.cinemabooking.services.UserService;
import com.group10.cinemabooking.services.events.UserRegistrationEmailEvent;
import com.group10.cinemabooking.utils.InAppCache;
import com.group10.cinemabooking.utils.LockManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private final PasswordEncoder passwordEncoder;
    private final LockManager<String> lockManager;
    private final InAppCache<Long, Users> userCache;
    private final InAppCache<String, Long> emailCache;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        if (userDto == null || userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new InvalidRequestException("User email must not be blank");
        }
        if (userDto.getPassword() == null || userDto.getPassword().isBlank()) {
            throw new InvalidRequestException("User password must not be blank");
        }
        String normalizedEmail = userDto.getEmail().trim().toLowerCase();
        Users user = new Users();
        ReentrantLock lock = lockManager.getLock("user:create:" + normalizedEmail);
        lock.lock();
        try {
            if (userRepository.existsByEmail(normalizedEmail)) {
                throw new InvalidRequestException("User email already exists");
            }
            user.setEmail(normalizedEmail);
            user.setFull_name(userDto.getFull_name());
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            saveUser(user);
            eventPublisher.publishEvent(new UserRegistrationEmailEvent(user.getUser_id()));
            return toDto(user);
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage());
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Users getUserById(Long id) {
        Users user = userCache.getOrLoad(id, key ->
                userRepository.findActiveById(key)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + key))
        );
        if (user.is_deleted()) {
            userCache.remove(id);
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        return user;
    }

    @Override
    @Transactional
    public UserDto updateUser(Long id, UserDto userDto) {
        Users user = getUserById(id);
        ReentrantLock lock = lockManager.getLock("user:update:" + id);
        lock.lock();
        try {
            if (userDto.getEmail() != null) user.setEmail(userDto.getEmail().trim().toLowerCase());
            if (userDto.getFull_name() != null) user.setFull_name(userDto.getFull_name());
            if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            }
            user.setUpdated_at(new Date());
            saveUser(user);
            return toDto(user);
        } catch (Exception e) {
            log.error("Error updating user with id {}: {}", id, e.getMessage());
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        ReentrantLock lock = lockManager.getLock("user:delete:" + id);
        lock.lock();
        try {
            Users user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
            if (user.is_deleted()) {
                throw new InvalidRequestException("User already deleted: " + id);
            }
            user.set_deleted(true);
            user.setUpdated_at(new Date());
            userRepository.save(user);
            if (userCache.contains(id)) userCache.remove(id);
            if (user.getEmail() != null) {
                emailCache.remove(user.getEmail());
            }
        } catch (Exception e) {
            log.error("Error deleting user with id {}: {}", id, e.getMessage());
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Transactional
    public UserDto restoreUser(Long id) {
        ReentrantLock lock = lockManager.getLock("user:restore:" + id);
        lock.lock();
        try {
            Users user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
            if (!user.is_deleted()) {
                throw new InvalidRequestException("User is not deleted: " + id);
            }
            user.set_deleted(false);
            user.setUpdated_at(new Date());
            saveUser(user);
            return toDto(user);
        } catch (Exception e) {
            log.error("Error restoring user with id {}: {}", id, e.getMessage());
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAllActive().stream().map(this::toDto).toList();
    }

    @Override
    public UserDto getUserDtoById(Long id) {
        return toDto(getUserById(id));
    }

    @Override
    public UserDto getUserDtoByEmail(String email) {
        return toDto(getUserByEmail(email));
    }

    @Override
    public Users getUserByEmail(String email) {
        long userId = getIdByEmail(email);
        if (userId <= 0) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        return getUserById(userId);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Users user;
        try {
            user = getUserByEmail(email);
        } catch (ResourceNotFoundException ex) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }
        return new User(
                user.getEmail(),
                user.getPassword(),
                List.of(() -> "ROLE_" + user.getRole().name())
        );
    }

    private void saveUser(Users user) {
        userRepository.save(user);
        userCache.put(user.getUser_id(), user);
        emailCache.put(user.getEmail(), user.getUser_id());
    }

    private long getIdByEmail(String email) {
        return emailCache.getOrLoad(email, key -> userRepository.getUserIdByEmail(key).orElse(0L));
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
        dto.set_deleted(user.is_deleted());
        return dto;
    }
}
