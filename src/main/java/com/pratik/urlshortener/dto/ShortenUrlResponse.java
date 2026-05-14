package com.pratik.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/*
 * DTO for shorten URL response.
 */
@Data
@AllArgsConstructor
public class ShortenUrlResponse {

    /*
     * Generated short URL.
     */
    private String shortUrl;
}