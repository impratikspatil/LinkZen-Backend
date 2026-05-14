package com.pratik.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import lombok.Data;

/*
 * DTO for shorten URL request.
 */
@Data
public class ShortenUrlRequest {

    /*
     * Original URL from client.
     * Validation rules:
     * - cannot be empty
     * - must be valid URL
     */
    @NotBlank(message = "Original URL cannot be empty")

    @URL(message = "Please provide valid URL")
    private String originalUrl;

    private String customAlias;

    private Integer expiryInDays;
}