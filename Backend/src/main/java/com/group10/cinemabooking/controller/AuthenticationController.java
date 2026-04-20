package com.group10.cinemabooking.controller;

import com.group10.cinemabooking.dtos.UserDto;
import com.group10.cinemabooking.services.AuthSessionService;
import com.group10.cinemabooking.services.JwtService;
import com.group10.cinemabooking.services.UserService;
import com.group10.cinemabooking.utils.InAppCache;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private static final String AUTH_COOKIE_NAME = "auth_token";

    public final UserService userService;
    public final JwtService jwtService;
    public final AuthenticationManager authenticationManager;
    public final InAppCache<String, String> tokenCache;
    public final AuthSessionService authSessionService;

    @Autowired
    public AuthenticationController(UserService userService,
                                    JwtService jwtService,
                                    AuthenticationManager authenticationManager,
                                    InAppCache<String, String> tokenCache,
                                    AuthSessionService authSessionService
    ) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.tokenCache = tokenCache;
        this.authSessionService = authSessionService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody UserDto userDto, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userDto.getEmail(),
                        userDto.getPassword()
                )
        );
        if (authentication.isAuthenticated()) {
            String token = jwtService.generateToken(userDto.getEmail());
            tokenCache.put(token, userDto.getEmail()); // whitelist
            ResponseCookie authCookie = ResponseCookie.from(AUTH_COOKIE_NAME, token)
                    .httpOnly(true)
                    .secure(false)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(60 * 60)
                    .build();
            response.addHeader("Set-Cookie", authCookie.toString());
            return ResponseEntity.ok(token);
        }
        return ResponseEntity.status(401).body("Invalid email or password");
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserDto userDto) {
        UserDto createdUser = userService.createUser(userDto);
        if (createdUser != null) {
            return ResponseEntity.ok("User registered successfully");
        } else {
            return ResponseEntity.status(400).body("Failed to create user");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = authSessionService.extractToken(request);

        if (token != null && !token.isBlank()) {
            tokenCache.remove(token); // invalidate
        }

        ResponseCookie clearCookie = ResponseCookie.from(AUTH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", clearCookie.toString());

        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(HttpServletRequest request) {
        UserDto user = authSessionService.getCurrentUser(request);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(user);
    }
}
