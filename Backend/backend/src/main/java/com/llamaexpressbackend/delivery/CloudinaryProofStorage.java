package com.llamaexpressbackend.delivery;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.llamaexpressbackend.common.error.BadGatewayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "cloudinary", matchIfMissing = true)
public class CloudinaryProofStorage implements ProofStorage {

    private final Cloudinary cloudinary;

    public CloudinaryProofStorage(@Value("${app.storage.cloudinary-url:}") String cloudinaryUrl) {
        if (cloudinaryUrl == null || cloudinaryUrl.isBlank()) {
            throw new IllegalStateException(
                    "CLOUDINARY_URL belum di-set. Isi di backend/.env, atau set app.storage.provider=fake untuk dev tanpa akun");
        }
        this.cloudinary = new Cloudinary(cloudinaryUrl);
    }

    @Override
    public StoredFile store(String orderCode, byte[] content, String contentType) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(content, ObjectUtils.asMap(
                    "folder", "llama-express/proofs",
                    "public_id", orderCode, // 1 foto per order; upload ulang = menimpa
                    "overwrite", true,
                    "resource_type", "image")); // Cloudinary menolak file yang bukan gambar
            return new StoredFile((String) result.get("secure_url"), (String) result.get("public_id"));
        } catch (Exception e) {
            log.error("Upload bukti {} ke Cloudinary gagal", orderCode, e);
            throw new BadGatewayException("UPLOAD_FAILED", "Gagal mengunggah foto, silakan coba lagi");
        }
    }
}