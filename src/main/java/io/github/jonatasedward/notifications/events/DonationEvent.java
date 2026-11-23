package io.github.jonatasedward.notifications.events;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Version 1 of the donation event published by the Mão Amiga API on {@code maoamiga.donations.v1}.
 * The event carries everything needed to notify people, so this service never calls the producer back.
 * See docs/events/donation-event.v1.schema.json.
 */
record DonationEvent(UUID eventId, String eventType, Instant occurredAt, Donation donation) {

    static final String REGISTERED = "DonationRegistered";
    static final String RECEIVED = "DonationReceived";
    static final String CANCELLED = "DonationCancelled";

    record Donation(UUID id, String item, Integer quantity, UUID pointId, String pointName,
                    UUID donorId, UUID responsibleId) {
    }

    void validate() {
        List<String> missing = new ArrayList<>();
        if (eventId == null) missing.add("eventId");
        if (eventType == null || eventType.isBlank()) missing.add("eventType");
        if (occurredAt == null) missing.add("occurredAt");
        if (donation == null) {
            missing.add("donation");
        } else {
            if (donation.id() == null) missing.add("donation.id");
            if (donation.item() == null || donation.item().isBlank()) missing.add("donation.item");
            if (donation.quantity() == null || donation.quantity() < 1) missing.add("donation.quantity");
            if (donation.pointName() == null || donation.pointName().isBlank()) missing.add("donation.pointName");
            if (donation.donorId() == null) missing.add("donation.donorId");
            if (donation.responsibleId() == null) missing.add("donation.responsibleId");
        }
        if (!missing.isEmpty()) {
            throw new InvalidEventException("event is missing or has invalid fields: " + String.join(", ", missing));
        }
    }
}
