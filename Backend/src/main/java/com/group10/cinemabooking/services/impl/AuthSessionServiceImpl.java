package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.dtos.UserDto;
import com.group10.cinemabooking.services.AuthSessionService;
import com.group10.cinemabooking.services.JwtService;
import com.group10.cinemabooking.services.UserService;
import com.group10.cinemabooking.utils.InAppCache;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthSessionServiceImpl implements AuthSessionService {
    private static final String AUTH_COOKIE_NAME = "auth_token";

    private final JwtService jwtService;
    private final UserService userService;
    private final InAppCache<String, String> tokenCache;

    @Override
    public String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public UserDto getCurrentUser(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null || token.isBlank() || !tokenCache.contains(token)) {
            return null;
        }

        String email = jwtService.extractEmail(token);
        if (email == null || email.isBlank()) {
            return null;
        }

        return userService.getUserDtoByEmail(email);
    }
}
