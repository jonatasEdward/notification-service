package io.github.jonatasedward.notifications.push;

import java.util.List;
import java.util.Set;

/** Delivers push messages and reports which device tokens the provider says are no longer valid. */
interface PushGateway {

    Set<String> send(List<PushMessage> messages);

    record PushMessage(String to, String title, String body, String referenceId) {
    }
}
