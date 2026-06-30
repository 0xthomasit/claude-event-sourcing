package com.example.shipping.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "shipping.ghn")
public class GhnProperties {
    private String baseUrl;
    private String token;
    private String shopId;
}
