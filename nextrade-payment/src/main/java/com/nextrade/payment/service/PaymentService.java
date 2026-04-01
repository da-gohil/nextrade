package com.nextrade.payment.service;

import com.nextrade.common.dto.PageResponse;
import com.nextrade.common.dto.event.*;
import com.nextrade.common.exception.BadRequestException;
import com.nextrade.common.exception.ConflictException;
import com.nextrade.common.exception.ResourceNotFoundException;
import com.nextrade.payment.dto.*;
import com.nextrade.payment.entity.*;
import com.nextrade.payment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;

    private final AtomicLong paymentCounter = new AtomicLong(1);
    private final Random random = new Random();

    @Transactional
    public PaymentDto processPayment(ProcessPaymentRequest request, Long userId) {
        // Idempotency check
        if (paymentRepository.existsByIdempotencyKey(request.getIdempotencyKey())) {
            Payment existing = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey()).orElseThrow();
            log.info("Idempotent payment request, returning existing: {}", existing.getPaymentNumber());
            return mapToDto(existing);
        }

        // Redis idempotency lock
        String redisKey = "idempotency:" + request.getIdempotencyKey();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(redisKey, "locked", 24, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(locked)) {
            throw new ConflictException("Duplicate payment request");
        }

        Payment.PaymentMethod method;
        try {
            method = Payment.PaymentMethod.valueOf(request.getMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid payment method: " + request.getMethod());
        }

        Payment payment = Payment.builder()
                .paymentNumber(generatePaymentNumber())
                .orderId(request.getOrderId())
                .userId(userId)
                .amount(request.getAmount())
                .method(method)
                .status(Payment.PaymentStatus.PROCESSING)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        payment = paymentRepository.save(payment);

        // Simulate payment processing (90% success rate)
        boolean success = random.nextInt(100) < 90;

        if (success) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setProcessedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            kafkaTemplate.send("payment.events", String.valueOf(request.getOrderId()),
                    new PaymentCompletedEvent(payment.getId(), request.getOrderId(), request.getAmount(), request.getMethod()));

            log.info("Payment {} COMPLETED for order {}", payment.getPaymentNumber(), request.getOrderId());
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason("Payment declined by bank");
            paymentRepository.save(payment);

            kafkaTemplate.send("payment.events", String.valueOf(request.getOrderId()),
                    new PaymentFailedEvent(payment.getId(), request.getOrderId(), "Payment declined by bank"));

            log.info("Payment {} FAILED for order {}", payment.getPaymentNumber(), request.getOrderId());
        }

        return mapToDto(payment);
    }

    public PaymentDto getPaymentByOrder(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment for order", orderId));
        return mapToDto(payment);
    }

    @Transactional
    public PaymentDto refundPayment(Long paymentId, RefundRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new BadRequestException("Cannot refund payment in status: " + payment.getStatus());
        }

        BigDecimal refundAmount = request.getAmount() != null ? request.getAmount() : payment.getAmount();

        Refund refund = Refund.builder()
                .payment(payment)
                .amount(refundAmount)
                .reason(request.getReason())
                .status(Refund.RefundStatus.COMPLETED)
                .build();
        refundRepository.save(refund);

        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        kafkaTemplate.send("notification.events", String.valueOf(payment.getUserId()),
                new NotificationEvent(payment.getUserId(), "PAYMENT_REFUNDED",
                        "Refund Processed",
                        "Refund of $" + refundAmount + " for payment " + payment.getPaymentNumber() + " has been processed.",
                        Map.of("paymentId", String.valueOf(paymentId))));

        log.info("Refund processed for payment {}", payment.getPaymentNumber());
        return mapToDto(payment);
    }

    public PageResponse<PaymentDto> getTransactions(Pageable pageable) {
        Page<Payment> page = paymentRepository.findAll(pageable);
        return PageResponse.<PaymentDto>builder()
                .content(page.getContent().stream().map(this::mapToDto).toList())
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
                .build();
    }

    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Payment service received ORDER_CREATED for order {}, auto-processing...", event.getOrderId());
        ProcessPaymentRequest request = new ProcessPaymentRequest();
        request.setOrderId(event.getOrderId());
        request.setAmount(event.getTotalAmount());
        request.setMethod("CREDIT_CARD");
        request.setIdempotencyKey("auto-" + event.getOrderId() + "-" + event.getOrderNumber());
        processPayment(request, event.getUserId());
    }

    private String generatePaymentNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("PAY-%s-%04d", date, paymentCounter.getAndIncrement());
    }

    private PaymentDto mapToDto(Payment p) {
        return PaymentDto.builder()
                .id(p.getId()).paymentNumber(p.getPaymentNumber())
                .orderId(p.getOrderId()).userId(p.getUserId())
                .amount(p.getAmount()).method(p.getMethod().name())
                .status(p.getStatus().name()).idempotencyKey(p.getIdempotencyKey())
                .failureReason(p.getFailureReason()).processedAt(p.getProcessedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
