package com.pratik.urlshortener.controller;

import com.pratik.urlshortener.model.Url;
import com.pratik.urlshortener.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.core.Authentication;


@RestController
public class RedirectController {

    @Autowired
    private UrlService urlService;


    /*
     * Redirect user to original URL.
     */
    @GetMapping("/{shortCode}")
    public RedirectView redirectToOriginalUrl(
            @PathVariable String shortCode,
            HttpServletRequest request
    ) {

        String ipAddress = request.getRemoteAddr();

        String userAgent =
                request.getHeader("User-Agent");

        String referer =
                request.getHeader("Referer");

        Url url = urlService.getUrlByShortCode(
                shortCode,
                ipAddress,
                userAgent,
                referer
        );

        return new RedirectView(url.getOriginalUrl());
    }

    @GetMapping("/all")
    public List<Url> getAllUrls(Authentication authentication) {

        String email = authentication.getName();

        return urlService.getAllUrls(email);

    }
}