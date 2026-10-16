package com.llamaexpressbackend.courier;

import com.llamaexpressbackend.common.error.NotFoundException;
import com.llamaexpressbackend.common.web.PageResponse;
import com.llamaexpressbackend.courier.dto.CourierProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCourierService {

    private final CourierProfileRepository profileRepository;
    private final CourierLocationStore locationStore;

    @Transactional(readOnly = true)
    public PageResponse<CourierProfileResponse> list(VerificationStatus status, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50),
                Sort.by(Sort.Direction.ASC, "createdAt"));
        return PageResponse
                .from(profileRepository.findByVerificationStatus(status, pageable).map(CourierProfileResponse::from));
    }

    @Transactional
    public CourierProfileResponse verify(Long courierId) {
        CourierProfile profile = find(courierId);
        profile.verify();
        return CourierProfileResponse.from(profile);
    }

    @Transactional
    public CourierProfileResponse reject(Long courierId) {
        CourierProfile profile = find(courierId);
        profile.reject(); // sekaligus memaksa offline
        locationStore.remove(courierId, profile.getVehicleType());
        return CourierProfileResponse.from(profile);
    }

    private CourierProfile find(Long courierId) {
        return profileRepository.findWithUserByUserId(courierId)
                .orElseThrow(() -> new NotFoundException("COURIER_NOT_FOUND", "Kurir tidak ditemukan"));
    }
}