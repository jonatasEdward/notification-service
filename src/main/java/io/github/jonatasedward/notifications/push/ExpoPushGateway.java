package io.github.jonatasedward.notifications.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.net.http.HttpClient;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sends through the Expo Push API. Expo answers with one ticket per message, in the same order,
 * and flags tokens of uninstalled apps as DeviceNotRegistered.
 */
class ExpoPushGateway implements PushGateway {

    private static final Logger log = LoggerFactory.getLogger(ExpoPushGateway.class);
    private static final int MAX_MESSAGES_PER_REQUEST = 100;

    private final RestClient client;

    ExpoPushGateway(RestClient.Builder builder, PushProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.timeout());
        this.client = builder.baseUrl(properties.expoUrl()).requestFactory(requestFactory).build();
    }

    @Override
    public Set<String> send(List<PushMessage> messages) {
        Set<String> invalidTokens = new HashSet<>();
        for (int start = 0; start < messages.size(); start += MAX_MESSAGES_PER_REQUEST) {
            List<PushMessage> batch = messages.subList(start, Math.min(start + MAX_MESSAGES_PER_REQUEST, messages.size()));
            invalidTokens.addAll(sendBatch(batch));
        }
        return invalidTokens;
    }

    private Set<String> sendBatch(List<PushMessage> batch) {
        List<Map<String, Object>> payload = batch.stream()
                .map(message -> Map.<String, Object>of(
                        "to", message.to(),
                        "title", message.title(),
                        "body", message.body(),
                        "sound", "default",
                        "data", Map.of("referenceId", String.valueOf(message.referenceId()))))
                .toList();

        JsonNode response = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        Set<String> invalidTokens = new HashSet<>();
        JsonNode tickets = response == null ? null : response.get("data");
        if (tickets == null || !tickets.isArray()) {
            return invalidTokens;
        }
        for (int i = 0; i < tickets.size() && i < batch.size(); i++) {
            JsonNode ticket = tickets.get(i);
            if (!"error".equals(ticket.path("status").asString())) {
                continue;
            }
            String error = ticket.path("details").path("error").asString();
            if ("DeviceNotRegistered".equals(error)) {
                invalidTokens.add(batch.get(i).to());
            } else {
                log.warn("Expo rejected a push message: {}", ticket.path("message").asString());
            }
        }
        return invalidTokens;
    }
}
