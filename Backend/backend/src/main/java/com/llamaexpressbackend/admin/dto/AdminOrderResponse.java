package com.llamaexpressbackend.admin.dto;

import com.llamaexpressbackend.order.dto.OrderResponse;
import com.llamaexpressbackend.payment.dto.PaymentResponse;

public record AdminOrderResponse(OrderResponse order, PartyInfo customer, PartyInfo courier,
        PaymentResponse latestPayment) {
}
