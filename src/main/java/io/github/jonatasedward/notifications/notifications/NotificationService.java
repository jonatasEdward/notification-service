package io.github.jonatasedward.notifications.notifications;

import io.github.jonatasedward.notifications.shared.error.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final Clock clock;

    NotificationService(NotificationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public List<Notification> createAll(List<NewNotification> notifications) {
        return repository.saveAll(notifications.stream()
                .map(data -> new Notification(data, clock.instant()))
                .toList());
    }

    @Transactional(readOnly = true)
    Page<Notification> listFor(UUID recipientId, Pageable pageable) {
        return repository.findByRecipientId(recipientId, pageable);
    }

    @Transactional(readOnly = true)
    long unreadCount(UUID recipientId) {
        return repository.countByRecipientIdAndReadAtIsNull(recipientId);
    }

    @Transactional
    void markRead(UUID id, UUID recipientId) {
        // Someone else's notification answers 404, not 403, so ids of other users are not confirmed.
        Notification notification = repository.findByIdAndRecipientId(id, recipientId)
                .orElseThrow(() -> new NotFoundException("notification not found"));
        notification.markRead(clock.instant());
    }

    @Transactional
    int markAllRead(UUID recipientId) {
        return repository.markAllRead(recipientId, clock.instant());
    }
}
