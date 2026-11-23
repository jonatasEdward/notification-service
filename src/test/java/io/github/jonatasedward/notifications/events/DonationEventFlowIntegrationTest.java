package io.github.jonatasedward.notifications.events;

import io.github.jonatasedward.notifications.support.IntegrationTest;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.ConsumerFactory;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DonationEventFlowIntegrationTest extends IntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    @Autowired
    private ConsumerFactory<?, ?> consumerFactory;

    @Test
    void registeredDonationNotifiesTheResponsibleForThePoint() throws Exception {
        UUID donationId = UUID.randomUUID();
        UUID responsible = UUID.randomUUID();

        publish(donationId.toString(),
                donationEvent(UUID.randomUUID(), "DonationRegistered", donationId, UUID.randomUUID(), responsible));

        await().atMost(TIMEOUT).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/notifications").with(userWithId(responsible)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.totalElements").value(1))
                        .andExpect(jsonPath("$.content[0].type").value("DONATION_REGISTERED"))
                        .andExpect(jsonPath("$.content[0].body").value("6 × Feijão 1kg para Ponto Central."))
                        .andExpect(jsonPath("$.content[0].referenceId").value(donationId.toString())));
    }

    @Test
    void redeliveredEventCreatesOnlyOneNotification() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID donationId = UUID.randomUUID();
        UUID donor = UUID.randomUUID();
        String event = donationEvent(eventId, "DonationReceived", donationId, donor, UUID.randomUUID());

        publish(donationId.toString(), event);
        publish(donationId.toString(), event);
        // A third, distinct event on the same key proves both copies above were consumed before we assert.
        publish(donationId.toString(),
                donationEvent(UUID.randomUUID(), "DonationReceived", donationId, donor, UUID.randomUUID()));

        await().atMost(TIMEOUT).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/notifications").with(userWithId(donor)))
                        .andExpect(jsonPath("$.totalElements").value(2)));
    }

    @Test
    void unknownEventTypesAreSkippedWithoutBlockingThePartition() throws Exception {
        UUID donationId = UUID.randomUUID();
        UUID responsible = UUID.randomUUID();

        publish(donationId.toString(),
                donationEvent(UUID.randomUUID(), "DonationRated", donationId, UUID.randomUUID(), responsible));
        publish(donationId.toString(),
                donationEvent(UUID.randomUUID(), "DonationCancelled", donationId, UUID.randomUUID(), responsible));

        await().atMost(TIMEOUT).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/notifications").with(userWithId(responsible)))
                        .andExpect(jsonPath("$.totalElements").value(1))
                        .andExpect(jsonPath("$.content[0].type").value("DONATION_CANCELLED")));
    }

    @Test
    void malformedMessageGoesToTheDeadLetterTopic() throws Exception {
        String key = "poison-" + UUID.randomUUID();
        publish(key, "{\"eventId\": \"not-a-uuid\", \"eventType\": \"DonationRegistered\"}");

        try (Consumer<?, ?> consumer = consumerFactory.createConsumer("dlt-check-" + UUID.randomUUID(), null)) {
            consumer.subscribe(List.of(DONATIONS_TOPIC + ".DLT"));
            await().atMost(TIMEOUT).until(() -> {
                for (ConsumerRecord<?, ?> record : consumer.poll(Duration.ofMillis(500))) {
                    if (key.equals(record.key())) {
                        assertThat(record.headers().lastHeader("kafka_dlt-exception-fqcn")).isNotNull();
                        return true;
                    }
                }
                return false;
            });
        }
    }
}
