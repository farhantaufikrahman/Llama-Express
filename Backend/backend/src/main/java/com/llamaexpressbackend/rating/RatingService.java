package com.llamaexpressbackend.rating;

import com.llamaexpressbackend.common.error.ConflictException;
import com.llamaexpressbackend.common.error.NotFoundException;
import com.llamaexpressbackend.courier.CourierProfile;
import com.llamaexpressbackend.courier.CourierProfileRepository;
import com.llamaexpressbackend.order.DeliveryOrder;
import com.llamaexpressbackend.order.DeliveryOrderRepository;
import com.llamaexpressbackend.order.OrderStatus;
import com.llamaexpressbackend.rating.dto.RateRequest;
import com.llamaexpressbackend.rating.dto.RatingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final DeliveryOrderRepository orderRepository;
    private final RatingRepository ratingRepository;
    private final CourierProfileRepository profileRepository;

    @Transactional
    public RatingResponse rate(Long customerId, String code, RateRequest req) {
        DeliveryOrder order = orderRepository.findByOrderCodeAndCustomer_Id(code, customerId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order tidak ditemukan"));
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ConflictException("ORDER_NOT_DELIVERED", "Rating hanya bisa diberikan setelah paket diterima");
        }
        if (ratingRepository.existsByOrder_Id(order.getId())) {
            throw new ConflictException("ALREADY_RATED", "Order ini sudah diberi rating");
        }

        Long courierId = order.getCourier().getId();
        CourierProfile profile = profileRepository.findForUpdate(courierId) // kunci baris profil kurir
                .orElseThrow(() -> new NotFoundException("COURIER_NOT_FOUND", "Kurir tidak ditemukan"));
        profile.addRating(req.score());
        ratingRepository.save(Rating.create(order, customerId, courierId, req.score(), req.review()));

        return new RatingResponse(code, req.score(), profile.getRatingAvg());
    }
}