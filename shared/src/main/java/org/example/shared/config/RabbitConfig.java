package org.example.shared.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "user.exchange";

    // Routing keys
    public static final String USER_CREATED_ROUTING_KEY = "user.created";
    public static final String WALLET_CREATED_ROUTING_KEY = "wallet.created";
    public static final String WALLET_FUNDED_ROUTING_KEY = "wallet.funded";
    public static final String WALLET_DEBIT_CONFIRMED_ROUTING_KEY = "wallet.debit.confirmed";
    public static final String PAYMENT_PROCESSED_ROUTING_KEY = "payment.processed";
    public static final String PAYMENT_CREATED_ROUTING_KEY = "payment.created";
    public static final String WALLET_RELEASE_FUNDED_ROUTING_KEY = "wallet.release.funded";
    public static final String NOTIFICATION_CREATED_ROUTING_KEY = "notification.created";

    // Queues
    public static final String USER_CREATED_QUEUE = "user.created.queue";
    public static final String WALLET_CREATED_QUEUE = "wallet.created.queue";
    public static final String WALLET_FUNDED_QUEUE = "wallet.funded.queue";
    public static final String WALLET_DEBIT_CONFIRMED_QUEUE = "wallet.debit.confirmed.queue";
    public static final String PAYMENT_CREATED_QUEUE = "payment.created.queue";
    public static final String WALLET_RELEASE_FUNDED_QUEUE = "wallet.release.funded.queue";
    public static final String NOTIFICATION_CREATED_QUEUE = "notification.created.queue";
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    // ======================
    // USER CREATED
    // ======================
    @Bean
    public Queue userCreatedQueue() {
        return QueueBuilder.durable(USER_CREATED_QUEUE).build();
    }

    @Bean
    public Binding userCreatedBinding() {
        return BindingBuilder
                .bind(userCreatedQueue())
                .to(exchange())
                .with(USER_CREATED_ROUTING_KEY);
    }

    // ======================
    // WALLET CREATED
    // ======================
    @Bean
    public Queue walletCreatedQueue() {
        return QueueBuilder.durable(WALLET_CREATED_QUEUE).build();
    }

    @Bean
    public Binding walletCreatedBinding() {
        return BindingBuilder
                .bind(walletCreatedQueue())
                .to(exchange())
                .with(WALLET_CREATED_ROUTING_KEY);
    }

    // ======================
    // WALLET FUNDED
    // ======================
    @Bean
    public Queue walletFundedQueue() {
        return QueueBuilder.durable(WALLET_FUNDED_QUEUE).build();
    }

    @Bean
    public Binding walletFundedBinding() {
        return BindingBuilder
                .bind(walletFundedQueue())
                .to(exchange())
                .with(WALLET_FUNDED_ROUTING_KEY);
    }

    // ======================
    // WALLET DEBIT CONFIRMED
    // ======================
    @Bean
    public Queue walletDebitConfirmedQueue() {
        return QueueBuilder.durable(WALLET_DEBIT_CONFIRMED_QUEUE).build();
    }

    @Bean
    public Binding walletDebitConfirmedBinding() {
        return BindingBuilder
                .bind(walletDebitConfirmedQueue())
                .to(exchange())
                .with(WALLET_DEBIT_CONFIRMED_ROUTING_KEY);
    }

    // ======================
    // WALLET PAYMENT CREATED
    // ======================
    @Bean
    public Queue paymentCreatedQueue() {
        return QueueBuilder.durable(PAYMENT_CREATED_QUEUE).build();
    }

    @Bean
    public Binding paymentCreatedBinding() {
        return BindingBuilder
                .bind(paymentCreatedQueue())
                .to(exchange())
                .with(PAYMENT_CREATED_ROUTING_KEY);
    }

    // ======================
    // WALLET RELEASE FUNDED
    // ======================
    @Bean
    public Queue walletReleaseFundedQueue() {
        return QueueBuilder.durable(WALLET_RELEASE_FUNDED_QUEUE).build();
    }

    @Bean
    public Binding walletReleaseFundedBinding() {
        return BindingBuilder
                .bind(walletReleaseFundedQueue())
                .to(exchange())
                .with(WALLET_RELEASE_FUNDED_ROUTING_KEY);
    }

    //=======================
    // NOTIFICATION CREATED
    //=======================
    @Bean
    public Queue notificationCreatedQueue() {
        return QueueBuilder.durable(NOTIFICATION_CREATED_QUEUE).build();
    }

    @Bean
    public Binding notificationCreatedBinding() {
        return BindingBuilder
                .bind(notificationCreatedQueue())
                .to(exchange())
                .with(NOTIFICATION_CREATED_ROUTING_KEY);
    }
}