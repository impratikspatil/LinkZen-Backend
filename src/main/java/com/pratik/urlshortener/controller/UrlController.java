package com.pratik.urlshortener.controller;

import com.pratik.urlshortener.dto.ShortenUrlRequest;
import com.pratik.urlshortener.dto.ShortenUrlResponse;
import com.pratik.urlshortener.model.Url;
import com.pratik.urlshortener.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import jakarta.validation.Valid;
import com.pratik.urlshortener.dto.UrlStatsResponse;


@RestController
@RequestMapping("/api/v1/url")
public class UrlController {

    @Autowired
    private UrlService urlService;

    /*
     * API to create short URL.
     */
    @PostMapping("/shorten")
    public ShortenUrlResponse createShortUrl(
            @Valid @RequestBody ShortenUrlRequest request
    ) {

        // Extract original URL
        String originalUrl = request.getOriginalUrl();

        // Call service layer
        Url savedUrl = urlService.createShortUrl(
                request.getOriginalUrl(),
                request.getCustomAlias(),
                request.getExpiryInDays()
        );

        // Create final short URL
        String shortUrl =
                "https://linkzen-backend-2.onrender.com/" + savedUrl.getShortCode();

        // Return response DTO
        return new ShortenUrlResponse(shortUrl);
    }


    @GetMapping("/stats/{shortCode}")
    public UrlStatsResponse getUrlStats(
            @PathVariable String shortCode
    ) {

        return urlService.getUrlStats(shortCode);
    }


}