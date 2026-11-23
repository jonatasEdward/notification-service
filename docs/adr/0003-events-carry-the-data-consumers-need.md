# 3. Events carry the data consumers need

Status: accepted

## Context

To write "Ponto Central confirmed receiving 6 × Feijão 1kg", this service needs the point name, the item, the quantity, and who to notify. It could receive only ids and call the Mão Amiga API back for the rest.

## Decision

Events carry the fields needed to notify people: item, quantity, point name, donor id and responsible id (event-carried state transfer).

## Consequences

- This service never calls the producer. If the Mão Amiga API is down, notifications for events already in Kafka still go out.
- No extra latency or load on the producer per event.
- The notification shows the data as it was when the event happened. If a point is renamed later, old notifications keep the old name, which is the correct behaviour for a history.
- The event contract is wider, so changing it needs care. The contract lives in `docs/events/donation-event.v1.schema.json`, and consumers ignore unknown fields and unknown event types so producers can add to it without breaking anyone.
