package org.example.shared.config;

public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String USER_CREATED = "user.created";
    public static final String WALLET_CREATED = "wallet.created";
    public static final String WALLET_FUNDED = "wallet.funded";
    public static final String WALLET_DEBIT_CONFIRMED = "wallet.debit.confirmed";
    public static final String PAYMENT_PROCESSED = "payment.processed";
    public static final String PAYMENT_CREATED = "payment.created";
    public static final String WALLET_RELEASE_FUNDED = "wallet.release.funded";
    public static final String NOTIFICATION_CREATED = "notification.created";

    public static final String topic = "wallet-service-topic";
}