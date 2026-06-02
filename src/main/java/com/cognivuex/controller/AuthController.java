package com.cognivuex.controller;

import com.cognivuex.dto.LoginRequestDTO;
import com.cognivuex.dto.RegisterRequestDTO;
import com.cognivuex.service.AuthService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public String register(
            @RequestBody RegisterRequestDTO request
    ) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public String login(
            @RequestBody LoginRequestDTO request
    ) {
        return authService.login(request);
    }
}