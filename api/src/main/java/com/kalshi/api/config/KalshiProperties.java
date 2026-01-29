package com.kalshi.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kalshi")
public record KalshiProperties (
        String baseUrl,
        String apiKeyId,
        String privateKeyPath
) {}
