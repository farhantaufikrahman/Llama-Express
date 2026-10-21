package com.llamaexpressbackend.delivery;

import com.llamaexpressbackend.common.security.CurrentUser;
import com.llamaexpressbackend.delivery.dto.CompleteDeliveryRequest;
import com.llamaexpressbackend.delivery.dto.ProofResponse;
import com.llamaexpressbackend.dispatch.dto.CourierJobResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/courier/jobs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CourierJobController {

    private final CourierJobService jobService;
    private final ProofService proofService;

    /** 200 + job, atau 204 kalau kurir sedang tidak mengantar apa pun */
    @GetMapping("/active")
    public ResponseEntity<CourierJobResponse> active(@AuthenticationPrincipal Jwt jwt) {
        return jobService.activeJob(CurrentUser.id(jwt))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{code}/start-pickup")
    public CourierJobResponse startPickup(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) {
        return jobService.startPickup(CurrentUser.id(jwt), code);
    }

    @PostMapping("/{code}/pickup")
    public CourierJobResponse pickUp(@AuthenticationPrincipal Jwt jwt, @PathVariable String code) {
        return jobService.pickUp(CurrentUser.id(jwt), code);
    }

    @PostMapping(value = "/{code}/proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProofResponse uploadProof(@AuthenticationPrincipal Jwt jwt, @PathVariable String code,
            @RequestPart("photo") MultipartFile photo,
            @RequestParam(value = "receivedBy", required = false) String receivedBy) {
        return proofService.upload(CurrentUser.id(jwt), code, photo, receivedBy);
    }

    @PostMapping("/{code}/complete")
    public CourierJobResponse complete(@AuthenticationPrincipal Jwt jwt, @PathVariable String code,
            @Valid @RequestBody CompleteDeliveryRequest req) {
        return jobService.complete(CurrentUser.id(jwt), code, req.verificationCode());
    }
}