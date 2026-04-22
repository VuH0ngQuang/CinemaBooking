package com.group10.cinemabooking.services;

import com.group10.cinemabooking.models.Users;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

public interface JwtService {
    String generateToken(String email);
    String extractEmail(String token);
    <T> T extractClaim(String token, Function<Claims, T> claimsResolver);
    Date extractExpiration(String token);
    Boolean validateToken(String token, Users user);
}
