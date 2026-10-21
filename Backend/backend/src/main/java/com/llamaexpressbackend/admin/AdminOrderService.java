package com.llamaexpressbackend.admin;

import com.llamaexpressbackend.admin.dto.*;
import com.llamaexpressbackend.common.error.NotFoundException;
import com.llamaexpressbackend.common.web.PageResponse;
import com.llamaexpressbackend.courier.CourierLocationStore;
import com.llamaexpressbackend.courier.CourierProperties;
import com.llamaexpressbackend.order.*;
import com.llamaexpressbackend.order.dto.OrderResponse;
import com.llamaexpressbackend.payment.PaymentRepository;
import com.llamaexpressbackend.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final DeliveryOrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final PaymentRepository paymentRepository;
    private final OrderTransitionRecorder transitions;
    private final CourierLocationStore locationStore;
    private final CourierProperties courierProperties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<AdminOrderSummary> search(AdminOrderFilter filter, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        var spec = AdminOrderSpecifications.matching(filter, courierProperties.businessZone(), null);
        return PageResponse.from(orderRepository.findAll(spec, pageable).map(AdminOrderSummary::from));
    }

    @Transactional(readOnly = true)
    public AdminOrderResponse detail(String code) {
        return toResponse(find(code));
    }

    @Transactional
    public AdminOrderResponse cancel(Long adminId, String code, String reason) {
        DeliveryOrder order = find(code);
        OrderStatus from = order.getStatus();
        Long courierId = (order.getCourier() != null) ? order.getCourier().getId() : null;

        order.cancelByAdmin(reason, clock.instant()); // 409 kalau state machine tidak mengizinkan
        transitions.record(order, from, adminId, "Dibatalkan admin: " + reason.trim());

        if (courierId != null) {
            locationStore.markAvailable(courierId);
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID && order.getPaymentMethod() == PaymentMethod.MIDTRANS) {
            log.warn("Order {} dibatalkan admin setelah lunas via Midtrans — PERLU REFUND", code);
        }
        return toResponse(order);
    }

    private DeliveryOrder find(String code) {
        return orderRepository.findByOrderCode(code)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order tidak ditemukan"));
    }

    private AdminOrderResponse toResponse(DeliveryOrder o) {
        OrderResponse order = OrderResponse.from(o, historyRepository.findByOrder_IdOrderByIdAsc(o.getId()));
        PaymentResponse payment = paymentRepository.findFirstByOrder_IdOrderByIdDesc(o.getId())
                .map(PaymentResponse::from).orElse(null);
        return new AdminOrderResponse(order, PartyInfo.from(o.getCustomer()), PartyInfo.from(o.getCourier()), payment);
    }
}