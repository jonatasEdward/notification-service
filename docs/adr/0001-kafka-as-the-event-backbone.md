# 1. Kafka as the event backbone

Status: accepted

## Context

Mão Amiga needs to tell people about things that happen to donations: a responsible person should hear about a new donation, a donor should hear that it arrived. The API that owns donations should not know how, or whether, anyone gets notified. Notification delivery can be slow (push providers) and can fail, and none of that should affect registering a donation.

We need an asynchronous channel between the producer and this service. The realistic options were RabbitMQ, Kafka, and a table in the producer's database polled by this service.

## Decision

Use Kafka. Donation events go to `maoamiga.donations.v1`, keyed by donation id.

- Keying by donation id puts every event of a donation on the same partition, so "registered" is always consumed before "received" or "cancelled" for that donation.
- Events stay in the log after they are consumed. A new consumer, or this one after a bug fix, can replay history.
- Consumer groups give horizontal scaling: more instances share the partitions without code changes.

## Consequences

- Delivery is at least once. The consumer has to be idempotent (see ADR 2).
- Ordering is only guaranteed per donation, not globally. Nothing here needs global order.
- Kafka is heavier to run than RabbitMQ. Locally it runs in Docker; the hosted demo uses a free managed cluster with low throughput limits, which is enough for a demo but not for the load test, which runs locally.
- The topic name carries a version. A breaking change to the event shape means a new topic (`.v2`) consumed in parallel, not an in-place change.
