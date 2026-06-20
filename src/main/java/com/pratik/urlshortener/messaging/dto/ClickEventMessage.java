package com.pratik.urlshortener.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/*
 * Message published to RabbitMQ queue
 * when a short URL is clicked.
 *
 * Contains all data needed to:
 * - Save UrlClick analytics record
 * - Increment click count on Url
 * - Update Redis cache
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickEventMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * Short code that was clicked.
     */
    private String shortCode;

    /*
     * Email of URL owner.
     */
    private String userEmail;

    /*
     * IP address of visitor.
     */
    private String ipAddress;

    /*
     * Browser/device info.
     */
    private String userAgent;

    /*
     * Request source/referrer.
     */
    private String referer;

    /*
     * Exact time of click.
     */
    private LocalDateTime clickedAt;
}
