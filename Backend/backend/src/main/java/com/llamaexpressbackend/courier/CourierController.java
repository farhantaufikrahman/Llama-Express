package com.llamaexpressbackend.courier;

import com.llamaexpressbackend.common.security.CurrentUser;
import com.llamaexpressbackend.courier.dto.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courier/me")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CourierController {

    private final CourierService courierService;

    @GetMapping
    public CourierProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        return courierService.me(CurrentUser.id(jwt));
    }

    @PatchMapping("/status")
    public CourierProfileResponse setStatus(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CourierStatusRequest req) {
        return courierService.setOnline(CurrentUser.id(jwt), req.online());
    }

    @PostMapping("/location")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateLocation(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody LocationUpdateRequest req) {
        courierService.updateLocation(CurrentUser.id(jwt), req);
    }
}