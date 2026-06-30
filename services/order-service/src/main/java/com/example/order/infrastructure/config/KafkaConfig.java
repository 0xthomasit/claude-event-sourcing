package com.example.order.infrastructure.config;

import com.example.common.events.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.converter.JsonMessageConverter;

@Configuration
public class KafkaConfig {

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
    @Bean public JsonMessageConverter jsonMessageConverter() {
        return new JsonMessageConverter();
    }
}
