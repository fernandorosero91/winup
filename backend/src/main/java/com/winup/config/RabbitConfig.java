package com.winup.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitConfig {
    public static final String DELAYED_EXCHANGE = "match.delayed";
    public static final String SIMULATION_QUEUE = "match_simulation_queue";
    public static final String NOTIFICATIONS_EXCHANGE = "notifications";
    public static final String FANOUT_EXCHANGE = "notifications.fanout";
    public static final String EMAIL_WIN_QUEUE = "q.email.win";
    public static final String EMAIL_LOSS_QUEUE = "q.email.loss";
    public static final String EMAIL_PROMO_QUEUE = "q.email.promo";
    public static final String INBOX_QUEUE = "q.notify.inbox";
    public static final String WEB_QUEUE = "q.notify.web";
    public static final String MOBILE_QUEUE = "q.notify.mobile";
    public static final String DEAD_LETTER_EXCHANGE = "winup.dlx";
    public static final String DEAD_LETTER_QUEUE = "winup.dlq";
    public static final String DEAD_LETTER_ROUTING_KEY = "dead";

    private Queue durableQueue(String name, String routingKey) {
        return QueueBuilder.durable(name)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY + "." + routingKey)
                .build();
    }

    @Bean
    public CustomExchange delayedExchange() {
        return new CustomExchange(
                DELAYED_EXCHANGE,
                "x-delayed-message",
                true,
                false,
                Map.of("x-delayed-type", "direct")
        );
    }

    @Bean
    public Queue simulationQueue() {
        return durableQueue(SIMULATION_QUEUE, "simulation");
    }

    @Bean
    public Binding simulationBinding(Queue simulationQueue, CustomExchange delayedExchange) {
        return BindingBuilder.bind(simulationQueue).to(delayedExchange).with("match.simulate").noargs();
    }

    @Bean
    public TopicExchange notificationsExchange() {
        return new TopicExchange(NOTIFICATIONS_EXCHANGE, true, false);
    }

    @Bean
    public FanoutExchange notificationsFanoutExchange() {
        return new FanoutExchange(FANOUT_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationInboxQueue() {
        return durableQueue(INBOX_QUEUE, "notify.inbox");
    }

    @Bean
    public Queue notificationWebQueue() {
        return durableQueue(WEB_QUEUE, "notify.web");
    }

    @Bean
    public Queue notificationMobileQueue() {
        return durableQueue(MOBILE_QUEUE, "notify.mobile");
    }

    @Bean
    public Binding inboxBinding(Queue notificationInboxQueue, FanoutExchange notificationsFanoutExchange) {
        return BindingBuilder.bind(notificationInboxQueue).to(notificationsFanoutExchange);
    }

    @Bean
    public Binding webBinding(Queue notificationWebQueue, FanoutExchange notificationsFanoutExchange) {
        return BindingBuilder.bind(notificationWebQueue).to(notificationsFanoutExchange);
    }

    @Bean
    public Binding mobileBinding(Queue notificationMobileQueue, FanoutExchange notificationsFanoutExchange) {
        return BindingBuilder.bind(notificationMobileQueue).to(notificationsFanoutExchange);
    }

    @Bean
    public Queue emailWinQueue() {
        return durableQueue(EMAIL_WIN_QUEUE, "email.win");
    }

    @Bean
    public Queue emailLossQueue() {
        return durableQueue(EMAIL_LOSS_QUEUE, "email.loss");
    }

    @Bean
    public Queue emailPromoQueue() {
        return durableQueue(EMAIL_PROMO_QUEUE, "email.promo");
    }

    @Bean
    public Binding emailPromoBinding(Queue emailPromoQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(emailPromoQueue).to(notificationsExchange).with("notify.promo");
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with("dead.#");
    }

    @Bean
    public Binding emailWinBinding(Queue emailWinQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(emailWinQueue).to(notificationsExchange).with("notify.email.win");
    }

    @Bean
    public Binding emailLossBinding(Queue emailLossQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(emailLossQueue).to(notificationsExchange).with("notify.email.loss");
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
