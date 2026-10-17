package com.llamaexpressbackend.admin;

import com.llamaexpressbackend.admin.dto.AdminDashboardResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/api/v1/admin/dashboard/summary")
    public AdminDashboardResponse summary() {
        return dashboardService.summary();
    }
}