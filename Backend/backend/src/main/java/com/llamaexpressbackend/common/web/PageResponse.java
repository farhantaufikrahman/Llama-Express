package com.llamaexpressbackend.common.web;

import org.springframework.data.domain.Page;
import java.util.List;

/**
 * Bentuk JSON paginasi yang stabil — jangan serialisasi Page<> Spring langsung
 * (strukturnya bisa berubah antar versi)
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <T> PageResponse<T> from(Page<T> p) {
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}