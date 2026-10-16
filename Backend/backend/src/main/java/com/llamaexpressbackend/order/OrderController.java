package com.llamaexpressbackend.order;

import com.llamaexpressbackend.common.security.CurrentUser;
import com.llamaexpressbackend.common.web.PageResponse;
import com.llamaexpressbackend.order.dto.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/quote")
    public QuoteResponse quote(@Valid @RequestBody QuoteRequest req) {
        return orderService.quote(req);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateOrderRequest req) {
        return orderService.create(CurrentUser.id(jwt), req);
    }

    @GetMapping
    public PageResponse<OrderSummaryResponse> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return orderService.list(CurrentUser.id(jwt), status, page, size);
    }

    @GetMapping("/{code}")
    public OrderResponse detail(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) {
        return orderService.detail(CurrentUser.id(jwt), code);
    }

    @PostMapping("/{code}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable String code,
            @Valid @RequestBody CancelOrderRequest req) {
        return orderService.cancel(CurrentUser.id(jwt), code, req.reason());
    }
}