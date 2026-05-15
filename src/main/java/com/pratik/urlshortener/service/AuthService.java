package com.pratik.urlshortener.service;

import com.pratik.urlshortener.dto.AuthResponse;
import com.pratik.urlshortener.dto.SignupRequest;
import com.pratik.urlshortener.model.User;
import com.pratik.urlshortener.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder;

    /*
     * Register new user.
     */
    public AuthResponse signup(
            SignupRequest request
    ) {

        /*
         * Check if email already exists.
         */
        if (userRepository.existsByEmail(
                request.getEmail()
        )) {

            throw new RuntimeException(
                    "Email already registered"
            );
        }

        /*
         * Create new user.
         */
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .build();


        userRepository.save(user);

        return new AuthResponse(
                "User registered successfully"
        );
    }
}