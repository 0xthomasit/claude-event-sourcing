package com.example.payment.domain;

import com.example.common.domain.model.Money;
import com.example.common.events.payment.*;
import com.example.payment.domain.model.Payment;
import com.example.payment.domain.model.PaymentMethod;
import com.example.payment.domain.model.PaymentMethodType;
import com.example.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Payment Aggregate")
class PaymentTest {

    private static final String ORDER_ID    = "order-001";
    private static final String CUSTOMER_ID = "customer-001";

    private Payment validPayment() {
        return Payment.initiate(
                ORDER_ID,
                CUSTOMER_ID,
                Money.of(new java.math.BigDecimal("150000"), "VND"),
                PaymentMethod.banking("****1234")
        );
    }

    @Test
    @DisplayName("initiate — raises PaymentInitiatedEvent and status is PENDING")
    void initiate_raisesEventAndStatusPending() {
        Payment payment = validPayment();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(payment.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(payment.getUncommittedEvents())
                .hasSize(1)
                .first().isInstanceOf(PaymentInitiatedEvent.class);
    }

    @Test
    @DisplayName("complete — PENDING → PROCESSING → COMPLETED raises PaymentCompletedEvent")
    void complete_raisesPaymentCompletedEvent() {
        Payment payment = validPayment();
        payment.clearUncommittedEvents();

        payment.startProcessing();
        payment.complete("TXN-REF-001");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getUncommittedEvents())
                .hasSize(2)
                .anySatisfy(e -> assertThat(e).isInstanceOf(PaymentCompletedEvent.class));
    }

    @Test
    @DisplayName("fail — PENDING → FAILED raises PaymentFailedEvent")
    void fail_raisesPaymentFailedEvent() {
        Payment payment = validPayment();
        payment.clearUncommittedEvents();

        payment.fail("Insufficient funds");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("Insufficient funds");
        assertThat(payment.getUncommittedEvents())
                .hasSize(1)
                .first().isInstanceOf(PaymentFailedEvent.class);
    }

    @Test
    @DisplayName("initiateRefund — COMPLETED → REFUND_INITIATED")
    void initiateRefund_fromCompleted_succeeds() {
        Payment payment = validPayment();
        payment.startProcessing();
        payment.complete("TXN-001");
        payment.clearUncommittedEvents();

        payment.initiateRefund("Order cancelled by customer");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_INITIATED);
        assertThat(payment.getUncommittedEvents())
                .hasSize(1)
                .first().isInstanceOf(RefundInitiatedEvent.class);
    }

    @Test
    @DisplayName("initiateRefund — from PENDING throws IllegalStateException")
    void initiateRefund_fromPending_throws() {
        Payment payment = validPayment();
        assertThatIllegalStateException()
                .isThrownBy(() -> payment.initiateRefund("reason"))
                .withMessageContaining("Cannot refund payment in status");
    }

    @Test
    @DisplayName("complete — from PENDING (not PROCESSING) throws IllegalStateException")
    void complete_fromPending_throws() {
        Payment payment = validPayment();
        assertThatIllegalStateException()
                .isThrownBy(() -> payment.complete("TXN-001"))
                .withMessageContaining("Cannot complete payment in status");
    }

    @Test
    @DisplayName("initiate — null orderId throws IllegalArgumentException")
    void initiate_nullOrderId_throws() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Payment.initiate(
                        null, CUSTOMER_ID,
                        Money.of(new java.math.BigDecimal("100"), "VND"),
                        PaymentMethod.card("****5678")))
                .withMessageContaining("orderId");
    }

    @Test
    @DisplayName("version increments with each event applied")
    void version_incrementsWithEachEvent() {
        Payment payment = validPayment();
        assertThat(payment.getVersion()).isEqualTo(1);

        payment.startProcessing();
        assertThat(payment.getVersion()).isEqualTo(2);

        payment.complete("TXN-001");
        assertThat(payment.getVersion()).isEqualTo(3);
    }
}
