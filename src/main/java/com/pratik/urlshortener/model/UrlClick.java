package com.pratik.urlshortener.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/*
 * Stores analytics data
 * for URL clicks.
 */
@Document(collection = "url_clicks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlClick {

    @Id
    private String id;

    /*
     * Short code clicked.
     */
    private String shortCode;

    /*
     * User IP address.
     */
    private String ipAddress;

    /*
     * Browser/device info.
     */
    private String userAgent;

    /*
     * Request source.
     */
    private String referer;

    /*
     * Click timestamp.
     */
    private LocalDateTime clickedAt;

    private String browser;

    private String operatingSystem;

    private String deviceType;
}