package io.github.jonatasedward.notifications.events;

import io.github.jonatasedward.notifications.notifications.NewNotification;
import io.github.jonatasedward.notifications.notifications.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DonationNotificationsTest {

    private final UUID donor = UUID.randomUUID();
    private final UUID responsible = UUID.randomUUID();

    @Test
    void registeredDonationGoesToTheResponsible() {
        List<NewNotification> notifications = DonationNotifications.from(event(DonationEvent.REGISTERED));

        assertThat(notifications).singleElement().satisfies(n -> {
            assertThat(n.recipientId()).isEqualTo(responsible);
            assertThat(n.type()).isEqualTo(NotificationType.DONATION_REGISTERED);
            assertThat(n.body()).isEqualTo("3 × Arroz 5kg para Feira do Cerrado.");
        });
    }

    @Test
    void receivedDonationGoesToTheDonor() {
        assertThat(DonationNotifications.from(event(DonationEvent.RECEIVED)))
                .singleElement()
                .satisfies(n -> assertThat(n.recipientId()).isEqualTo(donor));
    }

    @Test
    void cancelledDonationGoesToTheResponsible() {
        assertThat(DonationNotifications.from(event(DonationEvent.CANCELLED)))
                .singleElement()
                .satisfies(n -> assertThat(n.recipientId()).isEqualTo(responsible));
    }

    @Test
    void unknownTypesProduceNothing() {
        assertThat(DonationNotifications.from(event("DonationRated"))).isEmpty();
    }

    @Test
    void validationListsEveryProblemAtOnce() {
        DonationEvent broken = new DonationEvent(UUID.randomUUID(), DonationEvent.REGISTERED, Instant.now(),
                new DonationEvent.Donation(UUID.randomUUID(), "", 0, null, "Ponto", null, responsible));

        assertThatThrownBy(broken::validate)
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("donation.item")
                .hasMessageContaining("donation.quantity")
                .hasMessageContaining("donation.donorId");
    }

    private DonationEvent event(String type) {
        return new DonationEvent(UUID.randomUUID(), type, Instant.parse("2026-03-01T10:00:00Z"),
                new DonationEvent.Donation(UUID.randomUUID(), "Arroz 5kg", 3, UUID.randomUUID(), "Feira do Cerrado",
                        donor, responsible));
    }
}
