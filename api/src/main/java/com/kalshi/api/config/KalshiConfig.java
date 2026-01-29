package com.kalshi.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration 
@EnableConfigurationProperties(KalshiProperties.class)
public class KalshiConfig {}
