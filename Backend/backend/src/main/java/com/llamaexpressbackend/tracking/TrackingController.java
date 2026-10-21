package com.llamaexpressbackend.tracking;

import com.llamaexpressbackend.common.security.CurrentUser;
import com.llamaexpressbackend.tracking.dto.TrackingResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    /**
     * Snapshot saat halaman tracking dibuka; pembaruan berikutnya lewat WebSocket
     */
    @GetMapping("/api/v1/orders/{code}/tracking")
    @SecurityRequirement(name = "bearerAuth")
    public TrackingResponse forCustomer(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) {
        return trackingService.forCustomer(CurrentUser.id(jwt), code);
    }

    /** Publik — untuk penerima paket yang tidak punya akun */
    @GetMapping("/api/v1/track/{trackingToken}")
    public TrackingResponse publicTracking(@PathVariable String trackingToken) {
        return trackingService.forPublic(trackingToken);
    }
}