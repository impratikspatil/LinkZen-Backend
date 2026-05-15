package com.pratik.urlshortener.controller;

import com.pratik.urlshortener.dto.AuthResponse;
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

    /*
     * Signup API
     */
    @PostMapping("/signup")
    public AuthResponse signup(

            @Valid
            @RequestBody
            SignupRequest request
    ) {

        return authService.signup(request);
    }
}