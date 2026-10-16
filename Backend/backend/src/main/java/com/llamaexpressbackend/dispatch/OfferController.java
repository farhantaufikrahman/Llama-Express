package com.llamaexpressbackend.dispatch;

import com.llamaexpressbackend.common.security.CurrentUser;
import com.llamaexpressbackend.dispatch.dto.CourierJobResponse;
import com.llamaexpressbackend.dispatch.dto.OfferResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courier/offers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class OfferController {

    private final OfferService offerService;

    @GetMapping
    public List<OfferResponse> activeOffers(@AuthenticationPrincipal Jwt jwt) {
        return offerService.activeOffers(CurrentUser.id(jwt));
    }

    @PostMapping("/{offerId}/accept")
    public CourierJobResponse accept(@AuthenticationPrincipal Jwt jwt, @PathVariable Long offerId) {
        return offerService.accept(CurrentUser.id(jwt), offerId);
    }

    @PostMapping("/{offerId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long offerId) {
        offerService.reject(CurrentUser.id(jwt), offerId);
    }
}