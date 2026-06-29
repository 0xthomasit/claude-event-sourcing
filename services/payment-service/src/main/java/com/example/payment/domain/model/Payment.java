package com.example.payment.domain.model;

import com.example.common.domain.AggregateRoot;
import com.example.common.domain.model.Money;
import com.example.common.events.payment.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Payment Aggregate Root — CQRS + Event Sourcing.
 *
 * State is fully reconstructed by replaying domain events.
 * Every state transition raises a domain event FIRST, then applies it.
 *
 * Financial audit trail: events are NEVER deleted (Thông tư 09/2020/TT-NHNN).
 */
@Getter
public class Payment extends AggregateRoot {

    private UUID          id;
    private String        orderId;
    private String        customerId;
    private Money         amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String        failureReason;
    private String        transactionRef;   // external gateway reference
    private Instant       createdAt;
    private Instant       updatedAt;

    // No-arg constructor for reconstitution via event replay
    public Payment() {}

    // ─── Factory ──────────────────────────────────────────────────────────────

    /**
     * Initiate a new payment for an order.
     * Raises PaymentInitiatedEvent.
     */
    public static Payment initiate(String orderId, String customerId,
                                   Money amount, PaymentMethod paymentMethod) {
        if (orderId == null || orderId.isBlank())
            throw new IllegalArgumentException("orderId must not be blank");
        if (customerId == null || customerId.isBlank())
            throw new IllegalArgumentException("customerId must not be blank");
        if (amount == null)
            throw new IllegalArgumentException("amount must not be null");
        if (paymentMethod == null)
            throw new IllegalArgumentException("paymentMethod must not be null");

        Payment payment = new Payment();
        payment.id = UUID.randomUUID();

        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .aggregateId(payment.id.toString())
                .orderId(orderId)
                .customerId(customerId)
                .amount(amount.getAmount())
                .currency(amount.getCurrency())
                .paymentMethodType(paymentMethod.getType().name())
                .maskedAccount(paymentMethod.getMaskedAccount())
                .occurredOn(Instant.now())
                .build();

        payment.raiseEvent(event);
        payment.apply(event);
        return payment;
    }

    // ─── Commands ─────────────────────────────────────────────────────────────

    /**
     * Mark payment as processing (sent to payment gateway).
     */
    public void startProcessing() {
        if (status != PaymentStatus.PENDING)
            throw new IllegalStateException(
                    "Cannot start processing payment in status: " + status);

        PaymentProcessingEvent event = PaymentProcessingEvent.builder()
                .aggregateId(id.toString())
                .orderId(orderId)
                .occurredOn(Instant.now())
                .build();

        raiseEvent(event);
        apply(event);
    }

    /**
     * Complete payment successfully.
     * Raises PaymentCompletedEvent → consumed by order-service to confirm order.
     */
    public void complete(String transactionRef) {
        if (status != PaymentStatus.PROCESSING)
            throw new IllegalStateException(
                    "Cannot complete payment in status: " + status);

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .aggregateId(id.toString())
                .orderId(orderId)
                .amount(amount.getAmount())
                .occurredOn(Instant.now())
                .build();

        raiseEvent(event);
        apply(event);
        this.transactionRef = transactionRef;
    }

    /**
     * Fail payment with a reason.
     * Raises PaymentFailedEvent → consumed by order-service to cancel order (Saga rollback).
     */
    public void fail(String reason) {
        if (status != PaymentStatus.PROCESSING && status != PaymentStatus.PENDING)
            throw new IllegalStateException(
                    "Cannot fail payment in status: " + status);

        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .aggregateId(id.toString())
                .orderId(orderId)
                .failureReason(reason)
                .occurredOn(Instant.now())
                .build();

        raiseEvent(event);
        apply(event);
    }

    /**
     * Initiate a refund for a completed payment.
     */
    public void initiateRefund(String reason) {
        if (status != PaymentStatus.COMPLETED)
            throw new IllegalStateException(
                    "Cannot refund payment in status: " + status);

        RefundInitiatedEvent event = RefundInitiatedEvent.builder()
                .aggregateId(id.toString())
                .orderId(orderId)
                .amount(amount.getAmount())
                .reason(reason)
                .occurredOn(Instant.now())
                .build();

        raiseEvent(event);
        apply(event);
    }

    /**
     * Complete the refund.
     */
    public void completeRefund(String refundRef) {
        if (status != PaymentStatus.REFUND_INITIATED)
            throw new IllegalStateException(
                    "Cannot complete refund in status: " + status);

        RefundCompletedEvent event = RefundCompletedEvent.builder()
                .aggregateId(id.toString())
                .orderId(orderId)
                .amount(amount.getAmount())
                .refundRef(refundRef)
                .occurredOn(Instant.now())
                .build();

        raiseEvent(event);
        apply(event);
    }

    // ─── Event Apply (reconstitute state from events) ─────────────────────────

    public void apply(PaymentInitiatedEvent e) {
        this.id            = UUID.fromString(e.getAggregateId());
        this.orderId       = e.getOrderId();
        this.customerId    = e.getCustomerId();
        this.amount        = Money.of(e.getAmount(), e.getCurrency());
        this.paymentMethod = PaymentMethod.of(
                PaymentMethodType.valueOf(e.getPaymentMethodType()),
                e.getMaskedAccount());
        this.status    = PaymentStatus.PENDING;
        this.createdAt = e.getOccurredOn();
        this.updatedAt = e.getOccurredOn();
        incrementVersion();
    }

    public void apply(PaymentProcessingEvent e) {
        this.status    = PaymentStatus.PROCESSING;
        this.updatedAt = e.getOccurredOn();
        incrementVersion();
    }

    public void apply(PaymentCompletedEvent e) {
        this.status    = PaymentStatus.COMPLETED;
        this.updatedAt = e.getOccurredOn();
        incrementVersion();
    }

    public void apply(PaymentFailedEvent e) {
        this.status        = PaymentStatus.FAILED;
        this.failureReason = e.getFailureReason();
        this.updatedAt     = e.getOccurredOn();
        incrementVersion();
    }

    public void apply(RefundInitiatedEvent e) {
        this.status    = PaymentStatus.REFUND_INITIATED;
        this.updatedAt = e.getOccurredOn();
        incrementVersion();
    }

    public void apply(RefundCompletedEvent e) {
        this.status    = PaymentStatus.REFUNDED;
        this.updatedAt = e.getOccurredOn();
        incrementVersion();
    }
}
