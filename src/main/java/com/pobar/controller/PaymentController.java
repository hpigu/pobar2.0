package com.pobar.controller;

import com.pobar.common.Result;
import com.pobar.dto.payment.CheckoutRequest;
import com.pobar.dto.payment.CheckoutResponse;
import com.pobar.dto.payment.PaymentPreviewResponse;
import com.pobar.entity.User;
import com.pobar.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions/{sessionId}/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/preview")
    @PreAuthorize("hasAnyRole('WAITER','MANAGER','ADMIN')")
    public Result<PaymentPreviewResponse> preview(@PathVariable Integer sessionId) {
        return Result.ok(paymentService.preview(sessionId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('WAITER','MANAGER','ADMIN')")
    public Result<CheckoutResponse> checkout(
            @PathVariable Integer sessionId,
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal User operator) {
        return Result.ok(paymentService.checkout(sessionId, request, operator.getId()));
    }
}
