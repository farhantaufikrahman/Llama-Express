package com.llamaexpressbackend.rating;

import com.llamaexpressbackend.common.security.CurrentUser;
import com.llamaexpressbackend.rating.dto.RateRequest;
import com.llamaexpressbackend.rating.dto.RatingResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders/{code}/rating")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RatingResponse rate(@AuthenticationPrincipal Jwt jwt, @PathVariable String code,
            @Valid @RequestBody RateRequest req) {
        return ratingService.rate(CurrentUser.id(jwt), code, req);
    }
}