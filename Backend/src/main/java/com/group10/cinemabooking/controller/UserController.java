package com.group10.cinemabooking.controller;

import com.group10.cinemabooking.dtos.UserDto;
import com.group10.cinemabooking.enums.UserRoleEnum;
import com.group10.cinemabooking.models.Users;
import com.group10.cinemabooking.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userService.getUserDtoById(id));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable("email") String email) {
        return ResponseEntity.ok(userService.getUserDtoByEmail(email));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable("id") Long id,
                                              @RequestBody UserDto userDto) {
        return ResponseEntity.ok(userService.updateUser(id, userDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        requireSelfOrAdmin(id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<UserDto> restoreUser(@PathVariable("id") Long id) {
        requireAdmin();
        return ResponseEntity.ok(userService.restoreUser(id));
    }

    private void requireSelfOrAdmin(Long targetUserId) {
        Users principal = currentPrincipal();
        boolean isAdmin = principal.getRole() == UserRoleEnum.ADMIN;
        boolean isSelf  = principal.getUser_id() == targetUserId;
        if (!isAdmin && !isSelf) {
            throw new AccessDeniedException("You are not allowed to perform this action");
        }
    }

    private void requireAdmin() {
        Users principal = currentPrincipal();
        if (principal.getRole() != UserRoleEnum.ADMIN) {
            throw new AccessDeniedException("Admin role required");
        }
    }

    private Users currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Users principal)) {
            throw new AccessDeniedException("Authentication required");
        }
        return principal;
    }
}
