package io.github.jonatasedward.notifications.devices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class DeviceService {

    private final DeviceRepository repository;
    private final Clock clock;

    DeviceService(DeviceRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    void register(UUID userId, String pushToken, Device.Platform platform) {
        repository.findByPushToken(pushToken).ifPresentOrElse(
                device -> device.assignTo(userId, platform, clock.instant()),
                () -> repository.save(new Device(userId, pushToken, platform, clock.instant())));
    }

    @Transactional
    void unregister(UUID userId, String pushToken) {
        repository.deleteByPushTokenAndUserId(pushToken, userId);
    }

    @Transactional(readOnly = true)
    public List<Device> devicesOf(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return repository.findByUserIdIn(userIds);
    }

    @Transactional
    public void forget(Collection<String> pushTokens) {
        if (!pushTokens.isEmpty()) {
            repository.deleteByPushTokenIn(pushTokens);
        }
    }
}
