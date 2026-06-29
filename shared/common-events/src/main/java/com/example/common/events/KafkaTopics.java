package com.example.common.events;

/**
 * Centralized Kafka topic name constants.
 */
public final class KafkaTopics {

    // Order topics
    public static final String ORDER_PLACED    = "order.order.placed";
    public static final String ORDER_CONFIRMED = "order.order.confirmed";
    public static final String ORDER_CANCELLED = "order.order.cancelled";
    public static final String ORDER_SHIPPED   = "order.order.shipped";

    // Payment topics
    public static final String PAYMENT_COMPLETED = "payment.payment.completed";
    public static final String PAYMENT_FAILED    = "payment.payment.failed";

    // Inventory topics
    public static final String STOCK_RESERVED    = "inventory.stock.reserved";
    public static final String STOCK_RELEASED    = "inventory.stock.released";
    public static final String STOCK_REDUCED     = "inventory.stock.reduced";
    public static final String STOCK_REPLENISHED = "inventory.stock.replenished";
    public static final String STOCK_ADJUSTED    = "inventory.stock.adjusted";

    // Product topics
    public static final String PRODUCT_PRICE_UPDATED = "product.price.updated";

    private KafkaTopics() {}
}