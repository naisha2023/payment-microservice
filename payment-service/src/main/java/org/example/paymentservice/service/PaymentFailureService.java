package org.example.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.paymentservice.entity.Payment;
import org.example.paymentservice.enums.PaymentStatus;
import org.example.paymentservice.repository.PaymentRepository;
import org.example.paymentservice.constants.PaymentConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFailureService {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPaymentAsFailed(UUID paymentId, Exception ex) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException("Pago no encontrado: " + paymentId));

        log.error("Marcando pago como FAILED: {}", paymentId, ex);

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(truncate(
                PaymentConstants.ERROR_PROCESSING_PAYMENT + ": " + ex.getMessage(),
                PaymentConstants.MAX_FAILURE_REASON_LENGTH
        ));

        paymentRepository.save(payment);

        log.info("Pago {} marcado como FAILED correctamente", paymentId);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}