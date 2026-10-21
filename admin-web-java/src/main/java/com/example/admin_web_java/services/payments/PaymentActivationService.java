package com.example.adminwebjava.service.payment;

import com.example.adminwebjava.entity.OwnerSubscription;
import com.example.adminwebjava.entity.PaymentPlan;
import com.example.adminwebjava.entity.PaymentTransaction;
import com.example.adminwebjava.entity.TouristSubscription;
import com.example.adminwebjava.repository.OwnerSubscriptionRepository;
import com.example.adminwebjava.repository.PaymentTransactionRepository;
import com.example.adminwebjava.repository.TouristSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentActivationService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OwnerSubscriptionRepository ownerSubscriptionRepository;
    private final TouristSubscriptionRepository touristSubscriptionRepository;

    @Transactional
    public void markPaymentPaid(PaymentTransaction payment, String gatewayStatus) {
        if ("Paid".equalsIgnoreCase(payment.getStatus())) {
            return;
        }

        payment.setStatus("Paid");
        payment.setGatewayStatus((gatewayStatus == null || gatewayStatus.isBlank()) ? "PAID" : gatewayStatus);
        payment.setPaidAt(Instant.now());

        if (payment.getPaymentPlanId() != null && payment.getPaymentPlan() != null) {
            if ("Owner".equalsIgnoreCase(payment.getPayerType()) && payment.getOwnerProfileId() != null) {
                activateOwnerSubscription(payment);
            } else if ("Tourist".equalsIgnoreCase(payment.getPayerType()) && payment.getTouristId() != null) {
                activateTouristSubscription(payment);
            }
        }

        paymentTransactionRepository.save(payment);
    }

    private void activateOwnerSubscription(PaymentTransaction payment) {
        boolean exists = ownerSubscriptionRepository.existsByPaymentTransactionId(payment.getId());
        if (exists || payment.getOwnerProfileId() == null || payment.getPaymentPlan() == null || payment.getPaymentPlanId() == null) {
            return;
        }

        Optional latestActiveOpt = ownerSubscriptionRepository
                .findFirstByOwnerProfileIdAndStatusOrderByExpiresAtDesc(payment.getOwnerProfileId(), "Active");

        Instant now = Instant.now();
        Instant startsAt = now;

        if (latestActiveOpt.isPresent()) {
            Instant expiresAt = latestActiveOpt.get().getExpiresAt();
            if (expiresAt != null && expiresAt.isAfter(now)) {
                startsAt = expiresAt;
            }
        }

        PaymentPlan plan = payment.getPaymentPlan();
        int durationDays = (plan.getDurationDays() == null || plan.getDurationDays() <= 0) ? 30 : plan.getDurationDays();

        OwnerSubscription subscription = new OwnerSubscription();
        subscription.setOwnerProfileId(payment.getOwnerProfileId());
        subscription.setPaymentPlanId(payment.getPaymentPlanId());
        subscription.setPaymentTransactionId(payment.getId());
        subscription.setStatus("Active");
        subscription.setStartsAt(startsAt);
        subscription.setExpiresAt(startsAt.plus(durationDays, ChronoUnit.DAYS));

        ownerSubscriptionRepository.save(subscription);
    }

    private void activateTouristSubscription(PaymentTransaction payment) {
        boolean exists = touristSubscriptionRepository.existsByPaymentTransactionId(payment.getId());
        if (exists || payment.getTouristId() == null || payment.getPaymentPlan() == null || payment.getPaymentPlanId() == null) {
            return;
        }

        Optional latestActiveOpt = touristSubscriptionRepository
                .findFirstByTouristIdAndStatusOrderByExpiresAtDesc(payment.getTouristId(), "Active");

        Instant now = Instant.now();
        Instant startsAt = now;

        if (latestActiveOpt.isPresent()) {
            Instant expiresAt = latestActiveOpt.get().getExpiresAt();
            if (expiresAt != null && expiresAt.isAfter(now)) {
                startsAt = expiresAt;
            }
        }

        PaymentPlan plan = payment.getPaymentPlan();
        int durationDays = (plan.getDurationDays() == null || plan.getDurationDays() <= 0) ? 30 : plan.getDurationDays();

        TouristSubscription subscription = new TouristSubscription();
        subscription.setTouristId(payment.getTouristId());
        subscription.setPaymentPlanId(payment.getPaymentPlanId());
        subscription.setPaymentTransactionId(payment.getId());
        subscription.setStatus("Active");
        subscription.setStartsAt(startsAt);
        subscription.setExpiresAt(startsAt.plus(durationDays, ChronoUnit.DAYS));

        touristSubscriptionRepository.save(subscription);
    }
}