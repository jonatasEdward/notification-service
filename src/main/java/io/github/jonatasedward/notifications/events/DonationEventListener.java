package io.github.jonatasedward.notifications.events;

import io.github.jonatasedward.notifications.push.PushNotifier;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.util.Set;

@Component
class DonationEventListener {

    private static final Logger log = LoggerFactory.getLogger(DonationEventListener.class);
    private static final Set<String> KNOWN_TYPES =
            Set.of(DonationEvent.REGISTERED, DonationEvent.RECEIVED, DonationEvent.CANCELLED);

    // Tolerant reader: fields added by newer producers are ignored instead of breaking the consumer.
    private static final JsonMapper JSON = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final DonationEventProcessor processor;
    private final PushNotifier pushNotifier;
    private final Clock clock;
    private final Timer endToEndLatency;
    private final Counter duplicates;
    private final Counter ignored;

    DonationEventListener(DonationEventProcessor processor, PushNotifier pushNotifier, Clock clock,
                          MeterRegistry meters) {
        this.processor = processor;
        this.pushNotifier = pushNotifier;
        this.clock = clock;
        this.endToEndLatency = Timer.builder("notifications.event.latency")
                .description("Time from the event happening in the producer to the notification being stored")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meters);
        this.duplicates = Counter.builder("notifications.events.duplicate").register(meters);
        this.ignored = Counter.builder("notifications.events.ignored").register(meters);
    }

    @KafkaListener(topics = "${app.topics.donations}")
    void onDonationEvent(ConsumerRecord<String, String> record) {
        DonationEvent event = parse(record.value());

        if (!KNOWN_TYPES.contains(event.eventType())) {
            ignored.increment();
            log.info("ignoring event type {} (eventId={})", event.eventType(), event.eventId());
            return;
        }

        DonationEventProcessor.Outcome outcome = processor.process(event);
        if (outcome.duplicate()) {
            duplicates.increment();
            log.info("event {} already processed, skipping", event.eventId());
            return;
        }

        endToEndLatency.record(Duration.between(event.occurredAt(), clock.instant()));
        pushNotifier.notify(outcome.notifications());
    }

    private static DonationEvent parse(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new InvalidEventException("empty message");
        }
        DonationEvent event;
        try {
            event = JSON.readValue(payload, DonationEvent.class);
        } catch (JacksonException ex) {
            throw new InvalidEventException("message is not a valid donation event", ex);
        }
        event.validate();
        return event;
    }
}
