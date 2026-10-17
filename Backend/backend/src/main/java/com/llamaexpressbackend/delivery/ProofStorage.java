package com.llamaexpressbackend.delivery;

public interface ProofStorage {

    StoredFile store(String orderCode, byte[] content, String contentType);

    record StoredFile(String url, String publicId) {
    }
}