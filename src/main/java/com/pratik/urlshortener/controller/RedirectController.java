package com.pratik.urlshortener.controller;

import com.pratik.urlshortener.model.Url;
import com.pratik.urlshortener.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import jakarta.servlet.http.HttpServletRequest;
/*
 * Handles short URL redirects.
 */
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
}