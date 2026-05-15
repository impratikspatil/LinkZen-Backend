package com.pratik.urlshortener.controller;

import com.pratik.urlshortener.dto.AuthResponse;
import com.pratik.urlshortener.dto.LoginRequest;
import com.pratik.urlshortener.dto.SignupRequest;

import com.pratik.urlshortener.service.AuthService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public AuthResponse signup(

            @Valid
            @RequestBody
            SignupRequest request
    ) {

        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(

            @Valid
            @RequestBody
            LoginRequest request
    ) {

        return authService.login(request);
    }
}