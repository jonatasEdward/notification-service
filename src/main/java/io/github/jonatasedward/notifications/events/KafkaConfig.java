package io.github.jonatasedward.notifications.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
class KafkaConfig {

    @Bean
    NewTopic donationsTopic(TopicProperties topics) {
        return TopicBuilder.name(topics.donations()).partitions(topics.partitions()).build();
    }

    /** Same partition count as the source topic: the recoverer keeps the original partition number. */
    @Bean
    NewTopic donationsDeadLetterTopic(TopicProperties topics) {
        return TopicBuilder.name(topics.donationsDeadLetter()).partitions(topics.partitions()).build();
    }

    /**
     * Transient failures (database blip, lock timeout) get four attempts with growing waits.
     * A malformed event will never succeed, so it skips the retries and goes to the DLT right away,
     * instead of blocking its partition for everyone else.
     */
    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> kafkaTemplate) {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(500);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(5_000);

        DefaultErrorHandler handler = new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafkaTemplate), backOff);
        handler.addNotRetryableExceptions(InvalidEventException.class);
        return handler;
    }
}
