package com.llamaexpressbackend.courier;

import com.llamaexpressbackend.common.web.PageResponse;
import com.llamaexpressbackend.courier.dto.CourierProfileResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/couriers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminCourierController {

    private final AdminCourierService adminCourierService;

    @GetMapping
    public PageResponse<CourierProfileResponse> list(@RequestParam(defaultValue = "PENDING") VerificationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminCourierService.list(status, page, size);
    }

    @PatchMapping("/{courierId}/verify")
    public CourierProfileResponse verify(@PathVariable Long courierId) {
        return adminCourierService.verify(courierId);
    }

    @PatchMapping("/{courierId}/reject")
    public CourierProfileResponse reject(@PathVariable Long courierId) {
        return adminCourierService.reject(courierId);
    }
}