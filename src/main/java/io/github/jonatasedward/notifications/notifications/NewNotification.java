package io.github.jonatasedward.notifications.notifications;

import java.util.UUID;

/**
 * What a producer-specific translator hands over to be stored. Keeping this separate
 * from the entity lets the event side build notifications without touching JPA types.
 */
public record NewNotification(UUID recipientId, NotificationType type, String title, String body,
                              UUID referenceId, UUID eventId) {
}
