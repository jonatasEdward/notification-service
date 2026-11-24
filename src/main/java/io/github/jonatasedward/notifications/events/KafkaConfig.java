package io.github.jonatasedward.notifications.events;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    /**
     * Locally and in tests the service creates its topics. Managed clusters often forbid that (Aiven's
     * free plan rejects it by policy), so production sets app.topics.create=false and topics are created
     * in the provider's console instead.
     */
    @Bean
    @ConditionalOnProperty(name = "app.topics.create", havingValue = "true", matchIfMissing = true)
    NewTopic donationsTopic(TopicProperties topics) {
        return TopicBuilder.name(topics.donations()).partitions(topics.partitions()).build();
    }

    /** Same partition count as the source topic: the recoverer keeps the original partition number. */
    @Bean
    @ConditionalOnProperty(name = "app.topics.create", havingValue = "true", matchIfMissing = true)
    NewTopic donationsDeadLetterTopic(TopicProperties topics) {
        return TopicBuilder.name(topics.donationsDeadLetter()).partitions(topics.partitions()).build();
    }

    /**
     * Transient failures (database blip, lock timeout) get four attempts with growing waits.
     * A malformed event will never succeed, so it skips the retries and goes to the DLT right away,
     * instead of blocking its partition for everyone else.
     */
    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaOperations<?, ?> kafkaTemplate, TopicProperties topics) {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(500);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(5_000);

        // Explicit destination: the default suffix changed between Spring Kafka versions (.DLT vs -dlt),
        // and the DLT name is part of the operational contract, so it should not depend on a library default.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(topics.donationsDeadLetter(), record.partition()));

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(InvalidEventException.class);
        return handler;
    }
}
