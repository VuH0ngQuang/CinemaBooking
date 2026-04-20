package com.group10.cinemabooking.services;

import com.group10.cinemabooking.dtos.UserDto;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthSessionService {
    String extractToken(HttpServletRequest request);
    UserDto getCurrentUser(HttpServletRequest request);
}
