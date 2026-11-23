package io.github.jonatasedward.notifications.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "recipient_id", nullable = false, updatable = false)
    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private NotificationType type;

    @Column(nullable = false, length = 120, updatable = false)
    private String title;

    @Column(nullable = false, length = 500, updatable = false)
    private String body;

    @Column(name = "reference_id", updatable = false)
    private UUID referenceId;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    /**
     * The id is assigned here, not by the database, so Spring Data cannot tell a new notification from
     * an existing one and would merge it (one SELECT before every INSERT). Tracing the load test showed
     * exactly that; this flag makes save() go straight to persist.
     */
    @Transient
    private boolean isNew;

    protected Notification() {
    }

    Notification(NewNotification data, Instant createdAt) {
        this.isNew = true;
        this.id = UUID.randomUUID();
        this.recipientId = data.recipientId();
        this.type = data.type();
        this.title = data.title();
        this.body = data.body();
        this.referenceId = data.referenceId();
        this.eventId = data.eventId();
        this.createdAt = createdAt;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    void markRead(Instant now) {
        if (readAt == null) {
            readAt = now;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }
}
