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
}