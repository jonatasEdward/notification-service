package io.github.jonatasedward.notifications.push;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.push")
public record PushProperties(boolean expoEnabled, String expoUrl, Duration timeout) {
}
