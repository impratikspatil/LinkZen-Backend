package com.pratik.urlshortener.service;

import com.pratik.urlshortener.dto.AuthResponse;
import com.pratik.urlshortener.dto.SignupRequest;
import com.pratik.urlshortener.model.User;
import com.pratik.urlshortener.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.pratik.urlshortener.dto.LoginRequest;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder;


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


    public AuthResponse login(
            LoginRequest request
    ) {

        Optional<User> optionalUser =
                userRepository.findByEmail(
                        request.getEmail()
                );

        if (optionalUser.isEmpty()) {

            throw new RuntimeException(
                    "Invalid email or password"
            );
        }

        User user = optionalUser.get();

        boolean isPasswordMatch =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );

        if (!isPasswordMatch) {

            throw new RuntimeException(
                    "Invalid email or password"
            );
        }

        return new AuthResponse(
                "Login successful"
        );
    }
}