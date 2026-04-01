package com.nextrade.payment.controller;

import com.nextrade.common.dto.ApiResponse;
import com.nextrade.common.dto.PageResponse;
import com.nextrade.payment.dto.*;
import com.nextrade.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentDto>> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.status(201).body(ApiResponse.created(paymentService.processPayment(request, userId)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<PaymentDto>> getPayment(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentByOrder(orderId)));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<PaymentDto>> refund(
            @PathVariable Long id,
            @Valid @RequestBody RefundRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.refundPayment(id, request)));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<PageResponse<PaymentDto>>> transactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(paymentService.getTransactions(pageable)));
    }
}
