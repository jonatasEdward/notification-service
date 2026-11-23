package io.github.jonatasedward.notifications.push;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class PushConfig {

    @Bean
    PushGateway pushGateway(PushProperties properties) {
        return properties.expoEnabled() ? new ExpoPushGateway(RestClient.builder(), properties) : new LoggingPushGateway();
    }
}
