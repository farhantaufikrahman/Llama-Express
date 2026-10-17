package com.llamaexpressbackend.payment;

import com.llamaexpressbackend.common.security.CurrentUser;
import com.llamaexpressbackend.payment.dto.PaymentResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders/{code}/payment")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) {
        return paymentService.createPayment(CurrentUser.id(jwt), code);
    }

    @PostMapping("/sync")
    public PaymentResponse sync(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) {
        return paymentService.sync(CurrentUser.id(jwt), code);
    }
}