package com.llamaexpressbackend.courier;

import com.llamaexpressbackend.common.error.ConflictException;
import com.llamaexpressbackend.common.error.NotFoundException;
import com.llamaexpressbackend.courier.dto.CourierProfileResponse;
import com.llamaexpressbackend.courier.dto.LocationUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.llamaexpressbackend.tracking.CourierLocationEvent;
import com.llamaexpressbackend.tracking.CourierLocationPublisher;
import java.time.Clock;

@Service
@RequiredArgsConstructor
public class CourierService {

    private final CourierProfileRepository profileRepository;
    private final CourierLocationStore locationStore;
    private final CourierLocationPublisher locationPublisher;
    private final Clock clock;

    @Transactional(readOnly = true)
    public CourierProfileResponse me(Long courierId) {
        return CourierProfileResponse.from(find(courierId));
    }

    @Transactional
    public CourierProfileResponse setOnline(Long courierId, boolean online) {
        CourierProfile profile = find(courierId);
        if (online) {
            profile.goOnline(); // 403 kalau belum VERIFIED
        } else {
            profile.goOffline();
            locationStore.remove(courierId, profile.getVehicleType());
        }
        return CourierProfileResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public void updateLocation(Long courierId, LocationUpdateRequest req) {
        CourierProfile profile = find(courierId);
        if (!profile.isOnline()) {
            throw new ConflictException("COURIER_OFFLINE", "Aktifkan status online terlebih dahulu");
        }
        locationStore.update(courierId, profile.getVehicleType(), req.lat(), req.lng())
                .ifPresent(orderCode -> locationPublisher.publish(new CourierLocationEvent(
                        orderCode, courierId, req.lat(), req.lng(), req.speed(), req.heading(), clock.instant())));
    }
    // @Transactional(readOnly = true)
    // public void updateLocation(Long courierId, LocationUpdateRequest req) {
    // CourierProfile profile = find(courierId);
    // if (!profile.isOnline()) {
    // throw new ConflictException("COURIER_OFFLINE", "Aktifkan status online
    // terlebih dahulu");
    // }
    // locationStore.update(courierId, profile.getVehicleType(), req.lat(),
    // req.lng());
    // }

    private CourierProfile find(Long courierId) {
        return profileRepository.findWithUserByUserId(courierId)
                .orElseThrow(() -> new NotFoundException("COURIER_NOT_FOUND", "Profil kurir tidak ditemukan"));
    }
}