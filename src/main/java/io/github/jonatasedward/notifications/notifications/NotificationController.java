package io.github.jonatasedward.notifications.notifications;

import io.github.jonatasedward.notifications.shared.security.CurrentUser;
import io.github.jonatasedward.notifications.shared.web.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final NotificationService service;

    NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    PageResponse<NotificationResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal Jwt jwt) {
        return PageResponse.of(service.listFor(CurrentUser.id(jwt), PageRequest.of(page, size, NEWEST_FIRST)),
                NotificationResponse::from);
    }

    @GetMapping("/unread-count")
    UnreadCount unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return new UnreadCount(service.unreadCount(CurrentUser.id(jwt)));
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markRead(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        service.markRead(id, CurrentUser.id(jwt));
    }

    @PostMapping("/read-all")
    MarkedRead markAllRead(@AuthenticationPrincipal Jwt jwt) {
        return new MarkedRead(service.markAllRead(CurrentUser.id(jwt)));
    }

    record UnreadCount(long count) {
    }

    record MarkedRead(int updated) {
    }
}
