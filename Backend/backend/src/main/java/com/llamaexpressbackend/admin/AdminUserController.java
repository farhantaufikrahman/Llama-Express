package com.llamaexpressbackend.admin;

import com.llamaexpressbackend.admin.dto.AdminUserResponse;
import com.llamaexpressbackend.common.web.PageResponse;
import com.llamaexpressbackend.user.Role;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService userService;

    @GetMapping
    public PageResponse<AdminUserResponse> search(@RequestParam(defaultValue = "CUSTOMER") Role role,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return userService.search(role, q, page, size);
    }

    @PatchMapping("/{userId}/suspend")
    public AdminUserResponse suspend(@PathVariable Long userId) {
        return userService.suspend(userId);
    }

    @PatchMapping("/{userId}/activate")
    public AdminUserResponse activate(@PathVariable Long userId) {
        return userService.activate(userId);
    }
}