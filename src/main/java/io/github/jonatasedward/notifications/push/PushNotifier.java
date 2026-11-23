package io.github.jonatasedward.notifications.push;

import io.github.jonatasedward.notifications.devices.Device;
import io.github.jonatasedward.notifications.devices.DeviceService;
import io.github.jonatasedward.notifications.notifications.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Push is best effort. The notification is already stored when this runs, so a provider outage
 * costs the user a buzz on the phone, not the notification itself.
 */
@Service
public class PushNotifier {

    private static final Logger log = LoggerFactory.getLogger(PushNotifier.class);

    private final DeviceService deviceService;
    private final PushGateway gateway;

    PushNotifier(DeviceService deviceService, PushGateway gateway) {
        this.deviceService = deviceService;
        this.gateway = gateway;
    }

    public void notify(List<Notification> notifications) {
        if (notifications.isEmpty()) {
            return;
        }
        Set<UUID> recipients = notifications.stream().map(Notification::getRecipientId).collect(Collectors.toSet());
        Map<UUID, List<Device>> devicesByUser = deviceService.devicesOf(recipients).stream()
                .collect(Collectors.groupingBy(Device::getUserId));

        List<PushGateway.PushMessage> messages = notifications.stream()
                .flatMap(notification -> devicesByUser.getOrDefault(notification.getRecipientId(), List.of()).stream()
                        .map(device -> new PushGateway.PushMessage(device.getPushToken(), notification.getTitle(),
                                notification.getBody(), String.valueOf(notification.getReferenceId()))))
                .toList();
        if (messages.isEmpty()) {
            return;
        }

        try {
            deviceService.forget(gateway.send(messages));
        } catch (RuntimeException ex) {
            log.warn("push delivery failed for {} message(s); notifications remain available in the app",
                    messages.size(), ex);
        }
    }
}
