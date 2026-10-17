package com.llamaexpressbackend.courier.earnings;

import com.llamaexpressbackend.order.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Proyeksi ringan: hanya 3 kolom yang dibutuhkan, bukan seluruh entity order
 */
public record EarningRow(Instant deliveredAt, BigDecimal priceTotal, PaymentMethod paymentMethod) {
}