package io.github.jonatasedward.notifications.notifications;

import java.time.Instant;
import java.util.UUID;

record NotificationResponse(UUID id, NotificationType type, String title, String body, UUID referenceId,
                            Instant createdAt, boolean read) {

    static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getTitle(),
                notification.getBody(), notification.getReferenceId(), notification.getCreatedAt(),
                notification.getReadAt() != null);
    }
}
