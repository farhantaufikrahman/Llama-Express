package com.llamaexpressbackend.address.dto;

import com.llamaexpressbackend.address.Address;
import java.math.BigDecimal;

public record AddressResponse(Long id, String label, String address, BigDecimal lat, BigDecimal lng, String notes) {
    public static AddressResponse from(Address a) {
        return new AddressResponse(a.getId(), a.getLabel(), a.getLocation().getAddress(),
                a.getLocation().getLat(), a.getLocation().getLng(), a.getNotes());
    }
}