package com.llamaexpressbackend.admin;

import com.llamaexpressbackend.admin.dto.TariffRequest;
import com.llamaexpressbackend.admin.dto.TariffResponse;
import com.llamaexpressbackend.common.error.BadRequestException;
import com.llamaexpressbackend.common.error.ConflictException;
import com.llamaexpressbackend.order.Tariff;
import com.llamaexpressbackend.order.TariffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTariffService {

    private final TariffRepository tariffRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<TariffResponse> list() {
        Instant now = clock.instant();
        return tariffRepository.findAllByOrderByServiceTypeAscVehicleTypeAscActiveFromDesc().stream()
                .map(t -> TariffResponse.from(t, now)).toList();
    }

    @Transactional
    public TariffResponse create(TariffRequest req) {
        Instant now = clock.instant();
        Instant activeFrom = (req.activeFrom() != null) ? req.activeFrom().toInstant() : now;
        if (activeFrom.isBefore(now.minusSeconds(60))) {
            throw new BadRequestException("ACTIVE_FROM_IN_PAST",
                    "Tarif tidak boleh berlaku mundur — harga order lama harus tetap sesuai tarif saat itu");
        }
        if (req.minFare().compareTo(req.baseFare()) < 0) {
            throw new BadRequestException("INVALID_TARIFF", "Tarif minimum tidak boleh lebih kecil dari tarif dasar");
        }

        // Tutup tarif yang masih terbuka untuk kombinasi ini (dengan lock → aman dari
        // dua admin bersamaan)
        for (Tariff open : tariffRepository.findOpenForUpdate(req.serviceType(), req.vehicleType())) {
            if (!open.getActiveFrom().isBefore(activeFrom)) {
                throw new ConflictException("TARIFF_OVERLAP",
                        "Sudah ada tarif terjadwal yang mulai berlaku " + open.getActiveFrom());
            }
            open.closeAt(activeFrom);
        }
        Tariff saved = tariffRepository.save(Tariff.create(req.serviceType(), req.vehicleType(),
                req.baseFare(), req.perKmFare(), req.minFare(), activeFrom));
        return TariffResponse.from(saved, now);
    }
}