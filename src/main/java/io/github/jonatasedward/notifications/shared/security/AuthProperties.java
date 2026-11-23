package io.github.jonatasedward.notifications.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.auth")
public record AuthProperties(String jwksUri, String issuer) {
}
