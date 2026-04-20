package com.group10.cinemabooking.filter;

import com.group10.cinemabooking.exception.ResourceNotFoundException;
import com.group10.cinemabooking.services.JwtService;
import com.group10.cinemabooking.services.UserService;
import com.group10.cinemabooking.utils.InAppCache;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.rmi.ServerException;
import java.util.List;

@Component
public class JwtAuthFilter  extends OncePerRequestFilter {
    private static final String AUTH_COOKIE_NAME = "auth_token";
    private final UserService userService;
    private final JwtService jwtService;
    private final InAppCache<String, String> tokenCache;

    @Autowired
    public JwtAuthFilter(UserService userService,
                         JwtService jwtService,
                         InAppCache<String, String> tokenCache
    ) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.tokenCache = tokenCache;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token != null && !token.isBlank()) {
            //Check if token is in whitelist
            if (!tokenCache.contains(token)) {
                // Token not known/active → skip auth
                filterChain.doFilter(request, response);
                return;
            }
            String email = jwtService.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                com.group10.cinemabooking.models.Users user;
                try {
                    user = userService.getUserByEmail(email);
                } catch (ResourceNotFoundException ex) {
                    // user has been deleted (soft) - drop the token and proceed unauthenticated
                    tokenCache.remove(token);
                    filterChain.doFilter(request, response);
                    return;
                }
                if (user != null && jwtService.validateToken(token, user)) {
                    List<GrantedAuthority> authorities =
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
