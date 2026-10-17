package com.llamaexpressbackend.admin;

import com.llamaexpressbackend.admin.dto.*;
import com.llamaexpressbackend.common.security.CurrentUser;
import com.llamaexpressbackend.common.web.PageResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderController {

    private final AdminOrderService orderService;
    private final OrderCsvExporter csvExporter;

    @GetMapping
    public PageResponse<AdminOrderSummary> search(@ModelAttribute AdminOrderFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return orderService.search(filter, page, size);
    }

    /** Ditaruh SEBELUM "/{code}" supaya "export" tidak dianggap kode order */
    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> export(@ModelAttribute AdminOrderFilter filter) {
        StreamingResponseBody body = out -> csvExporter.write(filter, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"orders-" + LocalDate.now() + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    @GetMapping("/{code}")
    public AdminOrderResponse detail(@PathVariable String code) {
        return orderService.detail(code);
    }

    @PostMapping("/{code}/cancel")
    public AdminOrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable String code,
            @Valid @RequestBody AdminCancelRequest req) {
        return orderService.cancel(CurrentUser.id(jwt), code, req.reason());
    }
}