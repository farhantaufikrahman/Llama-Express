package com.llamaexpressbackend.admin;

import com.llamaexpressbackend.admin.dto.TariffRequest;
import com.llamaexpressbackend.admin.dto.TariffResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/tariffs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminTariffController {

    private final AdminTariffService tariffService;

    @GetMapping
    public List<TariffResponse> list() {
        return tariffService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TariffResponse create(@Valid @RequestBody TariffRequest req) {
        return tariffService.create(req);
    }
}