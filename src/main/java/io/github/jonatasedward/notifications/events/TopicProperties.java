package io.github.jonatasedward.notifications.events;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.topics")
public record TopicProperties(String donations, int partitions) {

    public String donationsDeadLetter() {
        return donations + ".DLT";
    }
}
