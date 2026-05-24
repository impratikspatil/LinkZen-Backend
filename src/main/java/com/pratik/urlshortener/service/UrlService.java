package com.pratik.urlshortener.service;

import com.pratik.urlshortener.dto.UrlStatsResponse;
import com.pratik.urlshortener.exception.CustomAliasAlreadyExistsException;
import com.pratik.urlshortener.exception.UrlExpiredException;
import com.pratik.urlshortener.model.Url;
import com.pratik.urlshortener.model.UrlClick;
import com.pratik.urlshortener.repository.UrlClickRepository;
import com.pratik.urlshortener.repository.UrlRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

@Service
public class UrlService {

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private UrlClickRepository urlClickRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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
         * Save click analytics
         */
        UrlClick urlClick = UrlClick.builder()
                .shortCode(shortCode)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .referer(referer)
                .clickedAt(LocalDateTime.now())
                .browser(detectBrowser(userAgent))
                .operatingSystem(detectOperatingSystem(userAgent))
                .deviceType(detectDeviceType(userAgent))
                .build();

        urlClickRepository.save(urlClick);

        /*
         * Increment click count
         */
        url.setClickCount(
                url.getClickCount() + 1
        );

        /*
         * Save updated click count
         * in MongoDB
         */
        urlRepository.save(url);

        /*
         * Update Redis cache also
         */
        redisTemplate
                .opsForValue()
                .set(shortCode, url);

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

    private String detectBrowser(
            String userAgent
    ) {

        if (userAgent == null) {
            return "Unknown";
        }

        if (userAgent.contains("Chrome")) {
            return "Chrome";
        }

        if (userAgent.contains("Firefox")) {
            return "Firefox";
        }

        if (userAgent.contains("Safari")) {
            return "Safari";
        }

        return "Other";
    }

    private String detectOperatingSystem(
            String userAgent
    ) {

        if (userAgent == null) {
            return "Unknown";
        }

        if (userAgent.contains("Windows")) {
            return "Windows";
        }

        if (userAgent.contains("Mac")) {
            return "MacOS";
        }

        if (userAgent.contains("Android")) {
            return "Android";
        }

        if (userAgent.contains("iPhone")) {
            return "iOS";
        }

        return "Other";
    }

    private String detectDeviceType(
            String userAgent
    ) {

        if (userAgent == null) {
            return "Unknown";
        }

        if (
                userAgent.contains("Mobile") ||
                        userAgent.contains("Android") ||
                        userAgent.contains("iPhone")
        ) {

            return "Mobile";
        }

        return "Desktop";
    }


}