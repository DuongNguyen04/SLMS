package com.example.slms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.jwt")
public class JwtConfig {

    private String secret = "replace-with-at-least-32-characters-secret-key";
    private long expirationMs = 86400000;
}
