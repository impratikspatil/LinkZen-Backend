package com.pratik.urlshortener.controller;

import com.pratik.urlshortener.dto.*;
import com.pratik.urlshortener.model.Url;
import com.pratik.urlshortener.service.JwtService;
import com.pratik.urlshortener.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import jakarta.validation.Valid;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.http.MediaType;


@RestController
@RequestMapping("/api/v1/url")
public class UrlController {

    @Autowired
    private UrlService urlService;

    @Autowired
    private JwtService jwtService;


    @PostMapping("/shorten")
    public ShortenUrlResponse createShortUrl(

            @Valid @RequestBody ShortenUrlRequest request,

            @RequestHeader("Authorization")
            String authHeader
    ) {

        String token =
                authHeader.substring(7);

        String email =
                jwtService.extractEmail(token);

        Url savedUrl = urlService.createShortUrl(
                request.getOriginalUrl(),
                request.getCustomAlias(),
                request.getExpiryInDays(),
                email
        );

        String shortUrl =
                "https://linkzen-backend-2.onrender.com/"
                        + savedUrl.getShortCode();

        return new ShortenUrlResponse(shortUrl);
    }


    @GetMapping("/all")
    public List<Url> getAllUrls(

            @RequestHeader("Authorization")
            String authHeader
    ) {

        String token =
                authHeader.substring(7);

        String email =
                jwtService.extractEmail(token);

        return urlService.getAllUrls(email);
    }


    @GetMapping("/stats/{shortCode}")
    public UrlStatsResponse getUrlStats(
            @PathVariable String shortCode
    ) {

        return urlService.getUrlStats(shortCode);
    }

    @DeleteMapping("/{shortCode}")
    public String deleteUrl(
            @PathVariable String shortCode,
            Authentication authentication
    ) {

        String email =
                authentication.getName();

        urlService.deleteUrl(
                shortCode,
                email
        );

        return "URL deleted successfully";
    }

    @PutMapping("/{shortCode}/expiry")
    public Url updateExpiry(
            @PathVariable String shortCode,
            @RequestBody UpdateExpiryRequest request,
            Authentication authentication
    ) {

        String email =
                authentication.getName();

        return urlService.updateExpiry(
                shortCode,
                request.getExpiryInDays(),
                email
        );
    }

    @GetMapping(
            value = "/qr/{shortCode}",
            produces = MediaType.IMAGE_PNG_VALUE
    )
    public ResponseEntity<byte[]> generateQrCode(
            @PathVariable String shortCode
    ) {

        byte[] qrCode =
                urlService.generateQrCode(shortCode);

        return ResponseEntity.ok(qrCode);
    }


    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics(
            Authentication authentication) {

        return ResponseEntity.ok(
                urlService.getAnalytics(authentication.getName())
        );
    }

        @GetMapping("/analytics/{shortCode}")
        public ResponseEntity<UrlAnalyticsResponse>
        getUrlAnalytics(
                @PathVariable String shortCode,
                Authentication authentication
        ) {

            return ResponseEntity.ok(

                    urlService.getUrlAnalytics(
                            shortCode,
                            authentication.getName()
                    )
            );
        }
}