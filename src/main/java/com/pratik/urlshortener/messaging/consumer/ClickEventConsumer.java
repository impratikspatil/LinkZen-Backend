package com.pratik.urlshortener.messaging.consumer;

import com.pratik.urlshortener.config.RabbitMQConfig;
import com.pratik.urlshortener.messaging.dto.ClickEventMessage;
import com.pratik.urlshortener.model.Url;
import com.pratik.urlshortener.model.UrlClick;
import com.pratik.urlshortener.repository.UrlClickRepository;
import com.pratik.urlshortener.repository.UrlRepository;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/*
 * Consumes click events from RabbitMQ queue.
 *
 * Responsibilities:
 * 1. Save UrlClick analytics record to MongoDB
 * 2. Increment clickCount on Url document
 * 3. Update Redis cache with new click count
 * 4. ACK message on success
 * 5. NACK on failure → retry → DLQ after max retries
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClickEventConsumer {

    private final UrlClickRepository urlClickRepository;

    private final UrlRepository urlRepository;

    private final RedisTemplate<String, Object> redisTemplate;

    private static final int MAX_RETRY_ATTEMPTS = 3;

    /*
     * Listen to main click events queue.
     * Manual ACK mode — we control when message is acknowledged.
     */
    @RabbitListener(
            queues = RabbitMQConfig.CLICK_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void consumeClickEvent(
            ClickEventMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(value = "x-death-count", required = false) Long deathCount
    ) throws IOException {

        log.info(
                "Processing click event for shortCode: {}",
                message.getShortCode()
        );

        try {

            /*
             * Step 1: Build and save UrlClick analytics record.
             */
            UrlClick urlClick = UrlClick.builder()
                    .shortCode(message.getShortCode())
                    .ipAddress(message.getIpAddress())
                    .userAgent(message.getUserAgent())
                    .referer(message.getReferer())
                    .clickedAt(message.getClickedAt())
                    .browser(detectBrowser(message.getUserAgent()))
                    .operatingSystem(detectOperatingSystem(message.getUserAgent()))
                    .deviceType(detectDeviceType(message.getUserAgent()))
                    .userEmail(message.getUserEmail())
                    .country(detectCountry(message.getIpAddress()))
                    .build();

            urlClickRepository.save(urlClick);

            /*
             * Step 2: Increment click count on Url document.
             */
            Url url = urlRepository
                    .findByShortCode(message.getShortCode())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "URL not found for shortCode: "
                                            + message.getShortCode()
                            )
                    );

            url.setClickCount(url.getClickCount() + 1);

            Url updatedUrl = urlRepository.save(url);

            /*
             * Step 3: Update Redis cache with new click count.
             */
            redisTemplate.opsForValue()
                    .set(message.getShortCode(), updatedUrl);

            /*
             * Step 4: ACK — message processed successfully.
             * Removed from queue.
             */
            channel.basicAck(deliveryTag, false);

            log.info(
                    "Click event processed successfully for shortCode: {}",
                    message.getShortCode()
            );

        } catch (Exception exception) {

            log.error(
                    "Failed to process click event for shortCode: {}. Error: {}",
                    message.getShortCode(),
                    exception.getMessage()
            );

            /*
             * Check how many times this message has been retried.
             */
            long retryCount = deathCount != null ? deathCount : 0;

            if (retryCount >= MAX_RETRY_ATTEMPTS) {

                /*
                 * Max retries exceeded.
                 * NACK without requeue → goes to DLQ.
                 */
                log.error(
                        "Max retries ({}) exceeded for shortCode: {}. Sending to DLQ.",
                        MAX_RETRY_ATTEMPTS,
                        message.getShortCode()
                );

                channel.basicNack(deliveryTag, false, false);

            } else {

                /*
                 * Retry attempt — requeue the message.
                 */
                log.warn(
                        "Retry attempt {} of {} for shortCode: {}",
                        retryCount + 1,
                        MAX_RETRY_ATTEMPTS,
                        message.getShortCode()
                );

                channel.basicNack(deliveryTag, false, true);
            }
        }
    }

    // ─── Helper Methods (same logic as UrlService) ─────────────────────

    private String detectBrowser(String userAgent) {

        if (userAgent == null) return "Unknown";

        if (userAgent.contains("Chrome")) return "Chrome";

        if (userAgent.contains("Firefox")) return "Firefox";

        if (userAgent.contains("Safari")) return "Safari";

        return "Other";
    }

    private String detectOperatingSystem(String userAgent) {

        if (userAgent == null) return "Unknown";

        if (userAgent.contains("Windows")) return "Windows";

        if (userAgent.contains("Mac")) return "MacOS";

        if (userAgent.contains("Android")) return "Android";

        if (userAgent.contains("iPhone")) return "iOS";

        return "Other";
    }

    private String detectDeviceType(String userAgent) {

        if (userAgent == null) return "Unknown";

        if (
                userAgent.contains("Mobile") ||
                        userAgent.contains("Android") ||
                        userAgent.contains("iPhone")
        ) {
            return "Mobile";
        }

        return "Desktop";
    }

    private String detectCountry(String ipAddress) {

        if (
                ipAddress == null ||
                        ipAddress.equals("127.0.0.1") ||
                        ipAddress.equals("0:0:0:0:0:0:0:1")
        ) {
            return "Local";
        }

        try {

            String url = "http://ip-api.com/json/" + ipAddress + "?fields=country";

            java.net.HttpURLConnection connection =
                    (java.net.HttpURLConnection)
                            new java.net.URL(url).openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            java.util.Scanner scanner =
                    new java.util.Scanner(
                            connection.getInputStream()
                    );

            String response = scanner.useDelimiter("\\A").next();
            scanner.close();

            String country = response
                    .replace("{", "")
                    .replace("}", "")
                    .replace("\"country\":", "")
                    .replace("\"", "")
                    .trim();

            return country.isEmpty() ? "Unknown" : country;

        } catch (Exception e) {
            return "Unknown";
        }
    }
}
