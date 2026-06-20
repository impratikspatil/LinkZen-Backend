package com.pratik.urlshortener.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    /*
     * Main queue where click events are published.
     */
    public static final String CLICK_QUEUE = "click.events.queue";

    /*
     * Dead Letter Queue — receives messages
     * that failed after max retries.
     */
    public static final String CLICK_DLQ = "click.events.dlq";

    /*
     * Exchange that routes messages to queues.
     */
    public static final String CLICK_EXCHANGE = "click.events.exchange";

    /*
     * Dead Letter Exchange.
     */
    public static final String CLICK_DLX = "click.events.dlx";

    /*
     * Routing key for click events.
     */
    public static final String CLICK_ROUTING_KEY = "click.event";

    /*
     * Max retry attempts before moving to DLQ.
     */
    public static final int MAX_RETRY_ATTEMPTS = 3;

    // ─── Main Queue ───────────────────────────────────────────

    /*
     * Main queue with DLQ configured.
     * If message fails after retries → goes to DLQ.
     */
    @Bean
    public Queue clickQueue() {

        Map<String, Object> args = new HashMap<>();

        /*
         * Route failed messages to Dead Letter Exchange.
         */
        args.put("x-dead-letter-exchange", CLICK_DLX);
        args.put("x-dead-letter-routing-key", CLICK_ROUTING_KEY);

        return new Queue(CLICK_QUEUE, true, false, false, args);
    }

    /*
     * Main exchange — routes messages to queues.
     */
    @Bean
    public DirectExchange clickExchange() {
        return new DirectExchange(CLICK_EXCHANGE);
    }

    /*
     * Bind main queue to main exchange via routing key.
     */
    @Bean
    public Binding clickBinding() {
        return BindingBuilder
                .bind(clickQueue())
                .to(clickExchange())
                .with(CLICK_ROUTING_KEY);
    }

    // ─── Dead Letter Queue ────────────────────────────────────

    /*
     * DLQ — stores messages that failed after MAX_RETRY_ATTEMPTS.
     * Can be inspected manually for debugging.
     */
    @Bean
    public Queue clickDeadLetterQueue() {
        return new Queue(CLICK_DLQ, true);
    }

    /*
     * Dead Letter Exchange.
     */
    @Bean
    public DirectExchange clickDeadLetterExchange() {
        return new DirectExchange(CLICK_DLX);
    }

    /*
     * Bind DLQ to DLX.
     */
    @Bean
    public Binding clickDeadLetterBinding() {
        return BindingBuilder
                .bind(clickDeadLetterQueue())
                .to(clickDeadLetterExchange())
                .with(CLICK_ROUTING_KEY);
    }

    // ─── Serialization ────────────────────────────────────────

    /*
     * Use JSON for message serialization.
     * Needed to serialize/deserialize ClickEventMessage.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /*
     * Configure RabbitTemplate with JSON converter.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory
    ) {

        RabbitTemplate rabbitTemplate =
                new RabbitTemplate(connectionFactory);

        rabbitTemplate.setMessageConverter(
                jsonMessageConverter()
        );

        return rabbitTemplate;
    }

    /*
     * Configure listener factory with JSON converter.
     * Used by @RabbitListener in consumer.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory
    ) {

        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);

        factory.setMessageConverter(jsonMessageConverter());

        /*
         * Manual ACK — only acknowledge after
         * successful DB save.
         */
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        return factory;
    }
}
