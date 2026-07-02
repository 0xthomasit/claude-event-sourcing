package com.example.inventory.infrastructure.config;

import com.example.common.events.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.converter.JacksonJsonMessageConverter;

@Configuration
public class KafkaConfig {

    @Bean public NewTopic stockReservedTopic() {
        return TopicBuilder.name(KafkaTopics.STOCK_RESERVED).partitions(6).replicas(1).build();
    }
    @Bean public NewTopic stockReleasedTopic() {
        return TopicBuilder.name(KafkaTopics.STOCK_RELEASED).partitions(3).replicas(1).build();
    }
    @Bean public NewTopic stockReducedTopic() {
        return TopicBuilder.name(KafkaTopics.STOCK_REDUCED).partitions(6).replicas(1).build();
    }
    @Bean public NewTopic stockReplenishedTopic() {
        return TopicBuilder.name(KafkaTopics.STOCK_REPLENISHED).partitions(3).replicas(1).build();
    }
    @Bean public NewTopic stockAdjustedTopic() {
        return TopicBuilder.name(KafkaTopics.STOCK_ADJUSTED).partitions(3).replicas(1).build();
    }
    @Bean public JacksonJsonMessageConverter JacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
