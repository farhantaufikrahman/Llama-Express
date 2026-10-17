package com.llamaexpressbackend.courier.earnings;

import com.llamaexpressbackend.common.security.CurrentUser;
import com.llamaexpressbackend.courier.earnings.dto.EarningsSummary;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CourierEarningsController {

    private final EarningsService earningsService;

    /** Contoh: /api/v1/courier/earnings?from=2026-10-01&to=2026-10-31 */
    @GetMapping("/api/v1/courier/earnings")
    public EarningsSummary earnings(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return earningsService.summary(CurrentUser.id(jwt), from, to);
    }
}