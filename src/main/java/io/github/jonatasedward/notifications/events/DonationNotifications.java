package io.github.jonatasedward.notifications.events;

import io.github.jonatasedward.notifications.notifications.NewNotification;
import io.github.jonatasedward.notifications.notifications.NotificationType;

import java.util.List;

/**
 * Decides who hears about each donation event and what they read. The texts are in Portuguese
 * because Mão Amiga's users are in Brazil; everything else in the service is in English.
 */
final class DonationNotifications {

    private DonationNotifications() {
    }

    static List<NewNotification> from(DonationEvent event) {
        DonationEvent.Donation donation = event.donation();
        String items = donation.quantity() + " × " + donation.item();

        return switch (event.eventType()) {
            case DonationEvent.REGISTERED -> List.of(new NewNotification(
                    donation.responsibleId(), NotificationType.DONATION_REGISTERED,
                    "Nova doação a caminho",
                    items + " para " + donation.pointName() + ".",
                    donation.id(), event.eventId()));
            case DonationEvent.RECEIVED -> List.of(new NewNotification(
                    donation.donorId(), NotificationType.DONATION_RECEIVED,
                    "Doação recebida",
                    donation.pointName() + " confirmou o recebimento de " + items + ". Obrigado!",
                    donation.id(), event.eventId()));
            case DonationEvent.CANCELLED -> List.of(new NewNotification(
                    donation.responsibleId(), NotificationType.DONATION_CANCELLED,
                    "Doação cancelada",
                    "O doador cancelou " + items + " para " + donation.pointName() + ".",
                    donation.id(), event.eventId()));
            default -> List.of();
        };
    }
}
