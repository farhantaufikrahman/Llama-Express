package com.llamaexpressbackend.delivery;

import com.llamaexpressbackend.common.error.BadRequestException;
import com.llamaexpressbackend.common.error.ConflictException;
import com.llamaexpressbackend.common.error.NotFoundException;
import com.llamaexpressbackend.delivery.dto.ProofResponse;
import com.llamaexpressbackend.order.DeliveryOrder;
import com.llamaexpressbackend.order.DeliveryOrderRepository;
import com.llamaexpressbackend.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProofService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final DeliveryOrderRepository orderRepository;
    private final ProofOfDeliveryRepository proofRepository;
    private final ProofStorage storage;
    private final TransactionTemplate tx; // transaksi manual — disediakan otomatis oleh Spring Boot
    private final Clock clock;

    /**
     * SENGAJA tanpa @Transactional — upload jaringan tidak boleh menahan koneksi DB
     */
    public ProofResponse upload(Long courierId, String code, MultipartFile photo, String receivedBy) {
        validate(photo, receivedBy);

        // Fase 1 — transaksi singkat: pastikan order milik kurir ini & sedang dalam
        // perjalanan
        tx.executeWithoutResult(s -> requireInTransit(courierId, code));

        // Fase 2 — TANPA transaksi: upload ke Cloudinary (bisa 1–5 detik)
        ProofStorage.StoredFile stored = storage.store(code, readBytes(photo), photo.getContentType());

        // Fase 3 — transaksi singkat: simpan / ganti catatan bukti
        return tx.execute(s -> {
            DeliveryOrder order = requireInTransit(courierId, code); // cek ulang: status bisa berubah saat upload
            Instant now = clock.instant();
            String who = (receivedBy == null || receivedBy.isBlank()) ? null : receivedBy.trim();
            ProofOfDelivery proof = proofRepository.findByOrder_Id(order.getId())
                    .map(existing -> {
                        existing.replace(stored.url(), stored.publicId(), who, now);
                        return existing;
                    })
                    .orElseGet(() -> proofRepository.save(
                            ProofOfDelivery.create(order, stored.url(), stored.publicId(), who, now)));
            return ProofResponse.from(order.getOrderCode(), proof);
        });
    }

    private DeliveryOrder requireInTransit(Long courierId, String code) {
        DeliveryOrder order = orderRepository.findByOrderCodeAndCourier_Id(code, courierId)
                .orElseThrow(() -> new NotFoundException("JOB_NOT_FOUND", "Pengantaran tidak ditemukan"));
        if (order.getStatus() != OrderStatus.IN_TRANSIT) {
            throw new ConflictException("NOT_IN_TRANSIT", "Foto bukti hanya bisa diunggah saat paket dalam perjalanan");
        }
        return order;
    }

    private static void validate(MultipartFile photo, String receivedBy) {
        if (photo == null || photo.isEmpty()) {
            throw new BadRequestException("PHOTO_REQUIRED", "Foto bukti wajib diunggah");
        }
        if (!ALLOWED_TYPES.contains(photo.getContentType())) {
            throw new BadRequestException("INVALID_FILE_TYPE", "Hanya file JPG, PNG, atau WEBP");
        }
        if (receivedBy != null && receivedBy.length() > 100) {
            throw new BadRequestException("VALIDATION_ERROR", "Nama penerima maksimal 100 karakter");
        }
    }

    private static byte[] readBytes(MultipartFile photo) {
        try {
            return photo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}