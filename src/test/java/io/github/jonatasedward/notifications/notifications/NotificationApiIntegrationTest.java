package io.github.jonatasedward.notifications.notifications;

import com.jayway.jsonpath.JsonPath;
import io.github.jonatasedward.notifications.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationApiIntegrationTest extends IntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Test
    void marksOneNotificationAsReadAndUpdatesTheUnreadCount() throws Exception {
        UUID user = UUID.randomUUID();
        createNotifications(user, 3);

        mockMvc.perform(get("/api/v1/notifications/unread-count").with(userWithId(user)))
                .andExpect(jsonPath("$.count").value(3));

        String body = mockMvc.perform(get("/api/v1/notifications").with(userWithId(user)))
                .andReturn().getResponse().getContentAsString();
        String firstId = JsonPath.read(body, "$.content[0].id");

        mockMvc.perform(post("/api/v1/notifications/{id}/read", firstId).with(userWithId(user)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/notifications/unread-count").with(userWithId(user)))
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void marksEverythingAsRead() throws Exception {
        UUID user = UUID.randomUUID();
        createNotifications(user, 2);

        mockMvc.perform(post("/api/v1/notifications/read-all").with(userWithId(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2));

        mockMvc.perform(get("/api/v1/notifications").with(userWithId(user)))
                .andExpect(jsonPath("$.content[0].read").value(true))
                .andExpect(jsonPath("$.content[1].read").value(true));
    }

    @Test
    void doesNotRevealNotificationsOfOtherUsers() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID notificationId = createNotifications(owner, 1).getFirst().getId();

        mockMvc.perform(post("/api/v1/notifications/{id}/read", notificationId).with(userWithId(UUID.randomUUID())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/notifications").with(userWithId(UUID.randomUUID())))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void requiresAToken() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registersAndRemovesADevice() throws Exception {
        UUID user = UUID.randomUUID();
        String token = "ExponentPushToken[" + UUID.randomUUID() + "]";

        mockMvc.perform(put("/api/v1/devices").with(userWithId(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pushToken\": \"%s\", \"platform\": \"ANDROID\"}".formatted(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/devices").with(userWithId(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pushToken\": \"%s\"}".formatted(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsSomethingThatIsNotAnExpoPushToken() throws Exception {
        mockMvc.perform(put("/api/v1/devices").with(userWithId(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pushToken\": \"not-a-token\", \"platform\": \"IOS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("pushToken"));
    }

    private List<Notification> createNotifications(UUID recipient, int count) {
        return notificationService.createAll(java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new NewNotification(recipient, NotificationType.DONATION_RECEIVED,
                        "Doação recebida", "Mensagem " + i, UUID.randomUUID(), UUID.randomUUID()))
                .toList());
    }
}
