package io.github.jonatasedward.notifications.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/** Used locally and in tests, where no message should leave the machine. */
class LoggingPushGateway implements PushGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushGateway.class);

    @Override
    public Set<String> send(List<PushMessage> messages) {
        messages.forEach(message -> log.info("push (not sent) to={} title={}", message.to(), message.title()));
        return Set.of();
    }
}
