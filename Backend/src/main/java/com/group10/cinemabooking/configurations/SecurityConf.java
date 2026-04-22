package com.group10.cinemabooking.configurations;

import com.group10.cinemabooking.filter.JwtAuthFilter;
import com.group10.cinemabooking.services.UserService;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@EnableWebSecurity
@Configuration
public class SecurityConf {

    public static final Logger log = LoggerFactory.getLogger(SecurityConf.class);
    public final JwtAuthFilter jwtAuthFilter;
    public final UserService userService;
    private final String corsAllowedOrigins;
    private final boolean corsAllowAll;
    private final boolean corsAllowCredentials;

    @Autowired
    public SecurityConf(
            JwtAuthFilter jwtAuthFilter,
            UserService userService,
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String corsAllowedOrigins,
            @Value("${app.cors.allow-all:false}") boolean corsAllowAll,
            @Value("${app.cors.allow-credentials:true}") boolean corsAllowCredentials) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userService = userService;
        this.corsAllowedOrigins = corsAllowedOrigins;
        this.corsAllowAll = corsAllowAll;
        this.corsAllowCredentials = corsAllowCredentials;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, PasswordEncoder passwordEncoder) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/webhook/**").permitAll()
                        .requestMatchers("/api/movies/**").permitAll()
                        .requestMatchers("/api/showtimes/**").permitAll()
                        .requestMatchers("/api/screeningrooms/**").permitAll()
                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider(passwordEncoder))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            log.warn("Access denied - User: {}, IP: {}, Path: {}, Method: {}, User-Agent: {}",
                                    request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous",
                                    getClientIp(request),
                                    request.getRequestURI(),
                                    request.getMethod(),
                                    request.getHeader("User-Agent"));
                            response.sendError(403, "Access Denied");
                        })
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("Unauthorized access - IP: {}, Path: {}, Method: {}, User-Agent: {}, Reason: {}",
                                    getClientIp(request),
                                    request.getRequestURI(),
                                    request.getMethod(),
                                    request.getHeader("User-Agent"),
                                    authException.getMessage());
                            response.sendError(401, "Unauthorized");
                        })
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        if (corsAllowAll) {
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isEmpty())
                    .collect(Collectors.toList());
            configuration.setAllowedOrigins(origins);
        }

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("*"));
        configuration.setAllowCredentials(corsAllowCredentials);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    private String getClientIp(HttpServletRequest request) {
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isEmpty()) {
            return cfConnectingIp;
        }
        String trueClientIp = request.getHeader("True-Client-IP");
        if (trueClientIp != null && !trueClientIp.isEmpty()) {
            return trueClientIp;
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

}
