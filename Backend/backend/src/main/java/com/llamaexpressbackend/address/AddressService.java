package com.llamaexpressbackend.address;

import com.llamaexpressbackend.address.dto.AddressRequest;
import com.llamaexpressbackend.address.dto.AddressResponse;
import com.llamaexpressbackend.common.error.ConflictException;
import com.llamaexpressbackend.common.error.NotFoundException;
import com.llamaexpressbackend.order.GeoPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private static final int MAX_ADDRESSES = 10;

    private final AddressRepository addressRepository;

    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        return addressRepository.findByUserIdOrderByCreatedAtAsc(userId).stream().map(AddressResponse::from).toList();
    }

    @Transactional
    public AddressResponse create(Long userId, AddressRequest req) {
        if (addressRepository.countByUserId(userId) >= MAX_ADDRESSES) {
            throw new ConflictException("ADDRESS_LIMIT", "Maksimal " + MAX_ADDRESSES + " alamat tersimpan");
        }
        Address saved = addressRepository.save(Address.create(userId, req.label(), toPoint(req), req.notes()));
        return AddressResponse.from(saved);
    }

    @Transactional
    public AddressResponse update(Long userId, Long id, AddressRequest req) {
        Address address = findOwned(userId, id);
        address.update(req.label(), toPoint(req), req.notes());
        return AddressResponse.from(address);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        addressRepository.delete(findOwned(userId, id));
    }

    /** Milik orang lain → 404 (bukan 403), supaya keberadaan id tidak bocor */
    private Address findOwned(Long userId, Long id) {
        return addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("ADDRESS_NOT_FOUND", "Alamat tidak ditemukan"));
    }

    private static GeoPoint toPoint(AddressRequest req) {
        return GeoPoint.of(req.address(), req.lat(), req.lng());
    }
}