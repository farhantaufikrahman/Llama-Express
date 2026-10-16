package com.llamaexpressbackend.order;

import com.llamaexpressbackend.common.error.NotFoundException;
import com.llamaexpressbackend.common.web.PageResponse;
import com.llamaexpressbackend.courier.CourierLocationStore;
import com.llamaexpressbackend.order.dto.*;
import com.llamaexpressbackend.user.User;
import com.llamaexpressbackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int MAX_PAGE_SIZE = 50;

    private final DeliveryOrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;
    private final OrderCodeGenerator codeGenerator;
    private final OrderTransitionRecorder transitions;
    private final CourierLocationStore locationStore;
    private final Clock clock;

    @Transactional(readOnly = true)
    public QuoteResponse quote(QuoteRequest req) {
        return pricingService.quote(req.serviceType(), req.vehicleType(), req.pickup(), req.dropoff(), req.weightKg());
    }

    @Transactional
    public OrderResponse create(Long customerId, CreateOrderRequest req) {
        QuoteResponse quote = pricingService.quote(req.serviceType(), req.vehicleType(),
                req.pickup(), req.dropoff(), req.weightKg());

        User customer = userRepository.getReferenceById(customerId);
        DeliveryOrder order = DeliveryOrder.place(customer, req, quote,
                codeGenerator.orderCode(), codeGenerator.trackingToken(), codeGenerator.verificationCode());
        orderRepository.save(order);

        transitions.record(order, null, customerId, "Order dibuat");
        transitions.recordCreated(order);

        if (order.getPaymentMethod() == PaymentMethod.CASH) {
            OrderStatus from = order.getStatus();
            order.startSearchingCourier();
            transitions.record(order, from, customerId, null);
        }
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> list(Long customerId, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DeliveryOrder> result = (status == null)
                ? orderRepository.findByCustomer_Id(customerId, pageable)
                : orderRepository.findByCustomer_IdAndStatus(customerId, status, pageable);
        return PageResponse.from(result.map(OrderSummaryResponse::from));
    }

    @Transactional(readOnly = true)
    public OrderResponse detail(Long customerId, String code) {
        return toResponse(findOwned(customerId, code));
    }

    @Transactional
    public OrderResponse cancel(Long customerId, String code, String reason) {
        DeliveryOrder order = findOwned(customerId, code);
        OrderStatus from = order.getStatus();
        Long assignedCourierId = (order.getCourier() != null) ? order.getCourier().getId() : null;

        order.cancelByCustomer(reason, clock.instant());
        transitions.record(order, from, customerId, reason);

        if (assignedCourierId != null) {
            locationStore.markAvailable(assignedCourierId); // kurir bebas lagi menerima order lain
        }
        return toResponse(order);
    }

    private DeliveryOrder findOwned(Long customerId, String code) {
        return orderRepository.findByOrderCodeAndCustomer_Id(code, customerId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order tidak ditemukan"));
    }

    private OrderResponse toResponse(DeliveryOrder order) {
        return OrderResponse.from(order, historyRepository.findByOrder_IdOrderByIdAsc(order.getId()));
    }
}