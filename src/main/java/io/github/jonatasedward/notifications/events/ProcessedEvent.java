package io.github.jonatasedward.notifications.events;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_events")
class ProcessedEvent implements Persistable<UUID> {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEvent() {
    }

    ProcessedEvent(UUID eventId, String eventType, Instant processedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = processedAt;
    }

    @Override
    public UUID getId() {
        return eventId;
    }

    /**
     * Always new, so save() issues an INSERT instead of a merge. If a concurrent consumer already
     * recorded the same event, the primary key rejects it and the transaction rolls back.
     */
    @Override
    public boolean isNew() {
        return true;
    }
}
