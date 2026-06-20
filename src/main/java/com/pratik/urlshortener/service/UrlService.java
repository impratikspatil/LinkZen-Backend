package com.pratik.urlshortener.service;

import com.pratik.urlshortener.dto.AnalyticsResponse;
import com.pratik.urlshortener.dto.UrlAnalyticsResponse;
import com.pratik.urlshortener.dto.UrlStatsResponse;
import com.pratik.urlshortener.exception.CustomAliasAlreadyExistsException;
import com.pratik.urlshortener.exception.UrlExpiredException;
import com.pratik.urlshortener.messaging.dto.ClickEventMessage;
import com.pratik.urlshortener.messaging.publisher.ClickEventPublisher;
import com.pratik.urlshortener.model.Url;
import com.pratik.urlshortener.model.UrlClick;
import com.pratik.urlshortener.repository.UrlClickRepository;
import com.pratik.urlshortener.repository.UrlRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.stream.Collectors;

@Service
public class UrlService {

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private UrlClickRepository urlClickRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /*
     * Injected to publish click events
     * to RabbitMQ queue asynchronously.
     */
    @Autowired
    private ClickEventPublisher clickEventPublisher;

    private static final String CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    /*
     * Generates random short code.
     */
    public String generateShortCode() {

        Random random = new Random();

        StringBuilder shortCode = new StringBuilder();

        for (int i = 0; i < 6; i++) {

            int index =
                    random.nextInt(
                            CHARACTERS.length()
                    );

            shortCode.append(
                    CHARACTERS.charAt(index)
            );
        }

        return shortCode.toString();
    }

    /*
     * Creates shortened URL and stores in MongoDB.
     */
    public Url createShortUrl(
            String originalUrl,
            String customAlias,
            Integer expiryInDays,
            String userEmail
    ) {

        String shortCode;

        /*
         * Use custom alias if provided.
         */
        if (
                customAlias != null &&
                        !customAlias.isBlank()
        ) {

            /*
             * Check alias uniqueness
             */
            if (
                    urlRepository.existsByShortCode(
                            customAlias
                    )
            ) {

                throw new CustomAliasAlreadyExistsException(
                        "Custom alias already exists"
                );
            }

            shortCode = customAlias;

        } else {

            /*
             * Generate unique random short code
             */
            do {

                shortCode =
                        generateShortCode();

            } while (
                    urlRepository.existsByShortCode(
                            shortCode
                    )
            );
        }

        LocalDateTime expiresAt = null;

        if (
                expiryInDays != null &&
                        expiryInDays > 0
        ) {

            expiresAt =
                    LocalDateTime.now()
                            .plusDays(expiryInDays);
        }

        Url url = Url.builder()
                .originalUrl(originalUrl)
                .shortCode(shortCode)
                .clickCount(0L)
                .createdAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .userEmail(userEmail)
                .build();

        return urlRepository.save(url);
    }


    public Url getUrlByShortCode(
            String shortCode,
            String ipAddress,
            String userAgent,
            String referer
    ) {

        Url url =
                (Url) redisTemplate
                        .opsForValue()
                        .get(shortCode);


        if (url == null) {

            url = urlRepository
                    .findByShortCode(shortCode)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Short URL not found"
                            )
                    );

            /*
             * Save in Redis cache
             */
            redisTemplate
                    .opsForValue()
                    .set(shortCode, url);
        }

        /*
         * Expiry check
         */
        if (
                url.getExpiresAt() != null &&
                        LocalDateTime.now()
                                .isAfter(
                                        url.getExpiresAt()
                                )
        ) {

            throw new UrlExpiredException(
                    "Short URL has expired"
            );
        }

        /*
         * Build click event message and publish to RabbitMQ.
         *
         * This is NON-BLOCKING — the redirect happens
         * immediately after this line.
         *
         * ClickEventConsumer will handle:
         * - Saving UrlClick analytics
         * - Incrementing click count
         * - Updating Redis cache
         */
        ClickEventMessage clickEvent = ClickEventMessage.builder()
                .shortCode(shortCode)
                .userEmail(url.getUserEmail())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .referer(referer)
                .clickedAt(LocalDateTime.now())
                .build();

        clickEventPublisher.publishClickEvent(clickEvent);

        return url;
    }

    /*
     * Get URL statistics
     */
    public UrlStatsResponse getUrlStats(
            String shortCode
    ) {

        Url url = urlRepository
                .findByShortCode(shortCode)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Short URL not found"
                        )
                );

        return new UrlStatsResponse(
                url.getOriginalUrl(),
                url.getShortCode(),
                url.getClickCount(),
                url.getCreatedAt()
        );
    }

    /*
     * Get all URLs for logged-in user
     */
    public List<Url> getAllUrls(
            String email
    ) {

        return urlRepository
                .findByUserEmailOrderByCreatedAtDesc(
                        email
                );
    }

    public void deleteUrl(
            String shortCode,
            String email
    ) {

        Url url = urlRepository
                .findByShortCode(shortCode)
                .orElseThrow(() ->
                        new RuntimeException("URL not found"));

        if (!url.getUserEmail().equals(email)) {

            throw new RuntimeException(
                    "Unauthorized access"
            );
        }

        urlRepository.delete(url);

        redisTemplate.delete(shortCode);
    }

    public Url updateExpiry(
            String shortCode,
            Integer expiryInDays,
            String email
    ) {

        Url url = urlRepository
                .findByShortCode(shortCode)
                .orElseThrow(() ->
                        new RuntimeException("URL not found"));

        if (!url.getUserEmail().equals(email)) {

            throw new RuntimeException(
                    "Unauthorized access"
            );
        }

        url.setExpiresAt(
                LocalDateTime.now()
                        .plusDays(expiryInDays)
        );

        Url updatedUrl =
                urlRepository.save(url);

        redisTemplate.opsForValue()
                .set(shortCode, updatedUrl);

        return updatedUrl;
    }


    public byte[] generateQrCode(
            String shortCode
    ) {

        try {

            String shortUrl =
                    "https://linkzen-backend-2.onrender.com/"
                            + shortCode;

            QRCodeWriter qrCodeWriter =
                    new QRCodeWriter();

            BitMatrix bitMatrix =
                    qrCodeWriter.encode(
                            shortUrl,
                            BarcodeFormat.QR_CODE,
                            300,
                            300
                    );

            BufferedImage bufferedImage =
                    new BufferedImage(
                            300,
                            300,
                            BufferedImage.TYPE_INT_RGB
                    );

            for (
                    int x = 0;
                    x < 300;
                    x++
            ) {

                for (
                        int y = 0;
                        y < 300;
                        y++
                ) {

                    bufferedImage.setRGB(
                            x,
                            y,
                            bitMatrix.get(x, y)
                                    ? 0x000000
                                    : 0xFFFFFF
                    );
                }
            }

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            ImageIO.write(
                    bufferedImage,
                    "png",
                    outputStream
            );

            return outputStream.toByteArray();

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Failed to generate QR code"
            );
        }
    }

    public AnalyticsResponse getAnalytics(String email) {

        List<UrlClick> clicks =
                urlClickRepository.findByUserEmail(email);

        Map<String, Long> browserStats =
                clicks.stream()
                        .collect(Collectors.groupingBy(
                                UrlClick::getBrowser,
                                Collectors.counting()
                        ));

        Map<String, Long> countryStats =

                clicks.stream()

                        .collect(Collectors.groupingBy(

                                click -> click.getCountry() == null

                                        ? "Unknown"

                                        : click.getCountry(),

                                Collectors.counting()

                        ));

        Map<String, Long> deviceStats =
                clicks.stream()
                        .collect(Collectors.groupingBy(
                                UrlClick::getDeviceType,
                                Collectors.counting()
                        ));

        Map<String, Long> weeklyClicks =
                new LinkedHashMap<>();

        for (int i = 6; i >= 0; i--) {

            String date =
                    LocalDate.now()
                            .minusDays(i)
                            .toString();

            long count =
                    clicks.stream()
                            .filter(click ->
                                    click.getClickedAt()
                                            .toLocalDate()
                                            .toString()
                                            .equals(date))
                            .count();

            weeklyClicks.put(date, count);
        }

        List<UrlClick> recentActivities =
                clicks.stream()
                        .sorted(
                                Comparator.comparing(
                                        UrlClick::getClickedAt
                                ).reversed()
                        )
                        .limit(10)
                        .toList();

        return AnalyticsResponse.builder()
                .browserStats(browserStats)
                .deviceStats(deviceStats)
                .countryStats(countryStats)
                .weeklyClicks(weeklyClicks)
                .recentActivities(recentActivities)
                .build();
    }

    public UrlAnalyticsResponse getUrlAnalytics(
            String shortCode,
            String email
    ) {

        Url url = urlRepository
                .findByShortCode(shortCode)
                .orElseThrow(() ->
                        new RuntimeException(
                                "URL not found"
                        )
                );

        if (!url.getUserEmail().equals(email)) {

            throw new RuntimeException(
                    "Unauthorized access"
            );
        }

        List<UrlClick> clicks =
                urlClickRepository.findByShortCode(
                        shortCode
                );

        Map<String, Long> browserStats =
                clicks.stream()
                        .collect(Collectors.groupingBy(
                                UrlClick::getBrowser,
                                Collectors.counting()
                        ));

        Map<String, Long> deviceStats =
                clicks.stream()
                        .collect(Collectors.groupingBy(
                                UrlClick::getDeviceType,
                                Collectors.counting()
                        ));

        List<UrlClick> recentActivities =
                clicks.stream()
                        .sorted(
                                Comparator.comparing(
                                        UrlClick::getClickedAt
                                ).reversed()
                        )
                        .limit(10)
                        .toList();

        return UrlAnalyticsResponse.builder()
                .shortCode(url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .clickCount(url.getClickCount())
                .createdAt(
                        url.getCreatedAt().toString()
                )
                .expiresAt(
                        url.getExpiresAt() == null
                                ? null
                                : url.getExpiresAt().toString()
                )
                .browserStats(browserStats)
                .deviceStats(deviceStats)
                .recentActivities(recentActivities)
                .build();
    }
}
