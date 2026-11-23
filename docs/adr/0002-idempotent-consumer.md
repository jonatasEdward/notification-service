# 2. Idempotent consumer with a processed-events table

Status: accepted

## Context

Kafka delivers at least once. The same event comes back if the consumer crashes after writing to the database but before committing the offset, or after a rebalance. Without protection, a donor would get the same "your donation arrived" notification twice.

## Decision

Record each `eventId` in `processed_events`, in the same database transaction that stores the notifications.

- If the id is already there, the event is skipped.
- The insert always goes through as an INSERT (never a merge), so if two attempts race, the primary key rejects one of them, its transaction rolls back, and the retry sees the event as processed.
- Push delivery runs after the transaction commits. A redelivered event never sends a second push, because it never gets past the duplicate check.

## Consequences

- Exactly-once effect for stored notifications, on top of an at-least-once transport, without Kafka transactions.
- The table grows with every event. It is cheap (one UUID per event), and a retention job can delete rows older than the topic's retention, since older events can no longer be redelivered. That job does not exist yet.
- Push is still at most once per event: if the process dies after the commit and before the push, the notification is in the app but the phone does not buzz. That trade-off is intentional (see ADR 4).
