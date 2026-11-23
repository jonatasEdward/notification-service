package io.github.jonatasedward.notifications.devices;

import io.github.jonatasedward.notifications.shared.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
class DeviceController {

    private final DeviceService service;

    DeviceController(DeviceService service) {
        this.service = service;
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void register(@Valid @RequestBody RegisterDevice request, @AuthenticationPrincipal Jwt jwt) {
        service.register(CurrentUser.id(jwt), request.pushToken(), request.platform());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unregister(@Valid @RequestBody UnregisterDevice request, @AuthenticationPrincipal Jwt jwt) {
        service.unregister(CurrentUser.id(jwt), request.pushToken());
    }

    record RegisterDevice(
            @NotBlank @Size(max = 200) @Pattern(regexp = "^(Exponent|Expo)PushToken\\[.+]$") String pushToken,
            @NotNull Device.Platform platform) {
    }

    record UnregisterDevice(@NotBlank @Size(max = 200) String pushToken) {
    }
}
