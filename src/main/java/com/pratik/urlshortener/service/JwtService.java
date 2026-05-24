package com.pratik.urlshortener.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {

    private static final String SECRET_KEY =
            "d7f3a8b1c9e4f6a2d8b0c7e5f1a9b3d6e7c1f4a8b9d2c6e5f7a1b3c8d9e2f4";

    /*
     * Generate JWT token
     */
    public String generateToken(String email) {

        return Jwts.builder()

                .setSubject(email)

                .setIssuedAt(new Date())

                .setExpiration(
                        new Date(
                                System.currentTimeMillis()
                                        + 1000 * 60 * 60 * 24
                        )
                )

                .signWith(
                        SignatureAlgorithm.HS256,
                        SECRET_KEY
                )

                .compact();
    }

    /*
     * Extract email from token
     */
    public String extractEmail(String token) {

        Claims claims = Jwts.parser()

                .setSigningKey(SECRET_KEY)

                .parseClaimsJws(token)

                .getBody();

        return claims.getSubject();
    }

    /*
     * Validate token
     */
    public boolean validateToken(String token) {

        try {

            Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token);

            return true;

        } catch (Exception e) {

            return false;
        }
    }
}