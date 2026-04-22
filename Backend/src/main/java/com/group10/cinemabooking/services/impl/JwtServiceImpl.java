package com.group10.cinemabooking.services.impl;

import com.group10.cinemabooking.configurations.AppConf;
import com.group10.cinemabooking.models.Users;
import com.group10.cinemabooking.services.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtServiceImpl implements JwtService {
    public final AppConf appConf;

    @Autowired
    public JwtServiceImpl(AppConf appConf) {
        this.appConf = appConf;
    }

    public String generateToken(String email){
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, email);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + appConf.getJwt().getExpirationMs()))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSigningKey() {
        String rawSecret = appConf.getJwt().getSecret();
        if (rawSecret == null || rawSecret.isBlank()) {
            throw new IllegalStateException("JWT secret is missing");
        }

        String secret = rawSecret.trim();
        if (secret.startsWith("0x") || secret.startsWith("0X")) {
            secret = secret.substring(2);
        }

        byte[] keyBytes;
        boolean isHex = secret.matches("^[0-9a-fA-F]+$") && (secret.length() % 2 == 0);
        if (isHex) {
            keyBytes = HexFormat.of().parseHex(secret);
        } else {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, Users user) {
        final String username = extractEmail(token);
        return (username.equals(user.getEmail()) && !isTokenExpired(token));
    }
}
