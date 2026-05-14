package com.pratik.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/*
 * DTO for URL statistics response.
 */
@Data
@AllArgsConstructor
public class UrlStatsResponse {

    /*
     * Original long URL.
     */
    private String originalUrl;

    /*
     * Generated short code.
     */
    private String shortCode;

    /*
     * Total number of clicks.
     */
    private Long clickCount;

    /*
     * URL creation timestamp.
     */
    private LocalDateTime createdAt;
}