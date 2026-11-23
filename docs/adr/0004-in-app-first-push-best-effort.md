# 4. In-app notifications are the source of truth, push is best effort

Status: accepted

## Context

There are two ways to reach a user: the notification list inside the app, and a push message through Expo. Push depends on an external provider, on the user having granted permission, and on the app still being installed. It does not work at all in the web version of the app.

## Decision

Every event produces stored notifications first. Push is attempted afterwards and its failures are logged, not retried and not allowed to fail the event.

- Tokens that Expo reports as `DeviceNotRegistered` are deleted, so dead tokens do not accumulate.
- Push is off by default and enabled per environment (`PUSH_EXPO_ENABLED`), so local runs and tests never send real messages.

## Consequences

- A push provider outage never blocks the partition or loses a notification.
- A user may occasionally miss a buzz on the phone while still seeing the notification in the app.
- If push becomes critical later (for example, time-sensitive alerts), the next step is a separate outbox for push deliveries with its own retries, instead of retrying inside the Kafka listener.
