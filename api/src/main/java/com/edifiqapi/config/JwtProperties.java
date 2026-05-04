package com.edifiqapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "edifiq.jwt")
public record JwtProperties(
        String secret,
        Duration accessTokenTtl
) {
}

