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
    public static final String ORDER_DELIVERED = "order.order.delivered";

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

    // Promotion topics
    public static final String PROMOTION_REDEEMED = "promotion.promotion.redeemed";
    public static final String PROMOTION_RELEASED = "promotion.promotion.released";

    // Invoice topics
    public static final String INVOICE_ISSUED = "invoice.invoice.issued";
    public static final String INVOICE_VOIDED = "invoice.invoice.voided";

    // Review topics
    public static final String REVIEW_SUBMITTED = "review.review.submitted";
    public static final String REVIEW_APPROVED  = "review.review.approved";

    private KafkaTopics() {}
}
