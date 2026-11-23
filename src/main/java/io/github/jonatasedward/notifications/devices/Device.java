package io.github.jonatasedward.notifications.devices;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "push_token", nullable = false, length = 200, unique = true)
    private String pushToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Platform platform;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Device() {
    }

    Device(UUID userId, String pushToken, Platform platform, Instant now) {
        this.id = UUID.randomUUID();
        this.pushToken = pushToken;
        assignTo(userId, platform, now);
    }

    /** A phone can change hands (log out, log in as someone else); the token follows the latest login. */
    void assignTo(UUID userId, Platform platform, Instant now) {
        this.userId = userId;
        this.platform = platform;
        this.updatedAt = now;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPushToken() {
        return pushToken;
    }

    public Platform getPlatform() {
        return platform;
    }

    public enum Platform {
        IOS,
        ANDROID
    }
}
