package io.github.jonatasedward.notifications.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
public abstract class IntegrationTest {

    protected static final String DONATIONS_TOPIC = "maoamiga.donations.v1";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected KafkaTemplate<String, String> kafkaTemplate;

    protected static RequestPostProcessor userWithId(UUID id) {
        return jwt().jwt(token -> token.subject(id.toString()).claim("papel", "DOADOR"));
    }

    protected void publish(String key, String payload) throws Exception {
        kafkaTemplate.send(DONATIONS_TOPIC, key, payload).get(10, TimeUnit.SECONDS);
    }

    protected static String donationEvent(UUID eventId, String type, UUID donationId, UUID donorId, UUID responsibleId) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "%s",
                  "occurredAt": "%s",
                  "donation": {
                    "id": "%s",
                    "item": "Feijão 1kg",
                    "quantity": 6,
                    "pointId": "%s",
                    "pointName": "Ponto Central",
                    "donorId": "%s",
                    "responsibleId": "%s"
                  }
                }
                """.formatted(eventId, type, Instant.now(), donationId, UUID.randomUUID(), donorId, responsibleId);
    }
}
