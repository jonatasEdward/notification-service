package io.github.jonatasedward.notifications.events;

import io.github.jonatasedward.notifications.notifications.Notification;
import io.github.jonatasedward.notifications.notifications.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Kafka delivers at least once, so the same event can arrive again after a rebalance or a crash
 * between processing and the offset commit. Recording the event id in the same transaction as the
 * notifications turns those redeliveries into no-ops.
 */
@Service
class DonationEventProcessor {

    private final ProcessedEventRepository processedEvents;
    private final NotificationService notificationService;
    private final Clock clock;

    DonationEventProcessor(ProcessedEventRepository processedEvents, NotificationService notificationService,
                           Clock clock) {
        this.processedEvents = processedEvents;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Transactional
    Outcome process(DonationEvent event) {
        if (processedEvents.existsById(event.eventId())) {
            return Outcome.duplicate();
        }
        processedEvents.save(new ProcessedEvent(event.eventId(), event.eventType(), clock.instant()));
        return Outcome.processed(notificationService.createAll(DonationNotifications.from(event)));
    }

    record Outcome(boolean duplicate, List<Notification> notifications) {

        static Outcome duplicate() {
            return new Outcome(true, List.of());
        }

        static Outcome processed(List<Notification> notifications) {
            return new Outcome(false, notifications);
        }
    }
}
