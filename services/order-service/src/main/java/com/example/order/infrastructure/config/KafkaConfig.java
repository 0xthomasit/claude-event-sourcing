package com.example.order.infrastructure.config;

import com.example.common.events.KafkaTopics;
import com.example.common.events.saga.SagaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.converter.JacksonJsonMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class KafkaConfig {

    // ── Domain Event Topics ──────────────────────────────────────────────────

    @Bean public NewTopic orderPlacedTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_PLACED).partitions(6).replicas(1).build();
    }
    @Bean public NewTopic orderConfirmedTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_CONFIRMED).partitions(6).replicas(1).build();
    }
    @Bean public NewTopic orderCancelledTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_CANCELLED).partitions(3).replicas(1).build();
    }
    @Bean public NewTopic orderShippedTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_SHIPPED).partitions(6).replicas(1).build();
    }
    @Bean public NewTopic orderDeliveredTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_DELIVERED).partitions(3).replicas(1).build();
    }

    // ── Saga Command/Reply Topics ────────────────────────────────────────────

    @Bean public NewTopic reserveStockCmdTopic() {
        return TopicBuilder.name(SagaTopics.RESERVE_STOCK_CMD).partitions(6).replicas(1).build();
    }
    @Bean public NewTopic reserveStockReplyTopic() {
        return TopicBuilder.name(SagaTopics.RESERVE_STOCK_REPLY).partitions(6).replicas(1).build();
    }
    @Bean public NewTopic releaseStockCmdTopic() {
        return TopicBuilder.name(SagaTopics.RELEASE_STOCK_CMD).partitions(3).replicas(1).build();
    }
    @Bean public NewTopic releaseStockReplyTopic() {
        return TopicBuilder.name(SagaTopics.RELEASE_STOCK_REPLY).partitions(3).replicas(1).build();
    }
    @Bean public NewTopic processPaymentCmdTopic() {
        return TopicBuilder.name(SagaTopics.PROCESS_PAYMENT_CMD).partitions(6).replicas(1).build();
    }
    @Bean public NewTopic processPaymentReplyTopic() {
        return TopicBuilder.name(SagaTopics.PROCESS_PAYMENT_REPLY).partitions(6).replicas(1).build();
    }

    @Bean public JacksonJsonMessageConverter JacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
