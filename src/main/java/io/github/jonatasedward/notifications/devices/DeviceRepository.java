package io.github.jonatasedward.notifications.devices;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByPushToken(String pushToken);

    List<Device> findByUserIdIn(Collection<UUID> userIds);

    long deleteByPushTokenAndUserId(String pushToken, UUID userId);

    long deleteByPushTokenIn(Collection<String> pushTokens);
}
