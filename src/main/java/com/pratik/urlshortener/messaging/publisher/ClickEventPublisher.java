package com.pratik.urlshortener.messaging.publisher;

import com.pratik.urlshortener.config.RabbitMQConfig;
import com.pratik.urlshortener.messaging.dto.ClickEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/*
 * Publishes click events to RabbitMQ queue.
 *
 * Called from UrlService when a short URL is clicked.
 * Non-blocking — redirect happens immediately after publish.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClickEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /*
     * Publish a click event to the main queue.
     *
     * RabbitMQ will deliver it to ClickEventConsumer
     * asynchronously.
     */
    public void publishClickEvent(ClickEventMessage message) {

        try {

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CLICK_EXCHANGE,
                    RabbitMQConfig.CLICK_ROUTING_KEY,
                    message
            );

            log.info(
                    "Click event published for shortCode: {}",
                    message.getShortCode()
            );

        } catch (Exception exception) {

            /*
             * Log the failure but do NOT throw.
             * Redirect must never fail because of queue issue.
             */
            log.error(
                    "Failed to publish click event for shortCode: {}. Error: {}",
                    message.getShortCode(),
                    exception.getMessage()
            );
        }
    }
}
