package com.group10.cinemabooking.controller;

import com.group10.cinemabooking.dtos.UserDto;
import com.group10.cinemabooking.services.JwtService;
import com.group10.cinemabooking.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    public final UserService userService;
    public final JwtService jwtService;
    public final AuthenticationManager authenticationManager;

    @Autowired
    public AuthenticationController(UserService userService,
                                    JwtService jwtService,
                                    AuthenticationManager authenticationManager
    ) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public String login(@RequestBody UserDto userDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userDto.getEmail(),
                        userDto.getPassword()
                )
        );
        if (authentication.isAuthenticated()) {
            return jwtService.generateToken(userDto.getEmail());
        } else {
            throw new RuntimeException("Invalid login credentials");
        }
    }
}
