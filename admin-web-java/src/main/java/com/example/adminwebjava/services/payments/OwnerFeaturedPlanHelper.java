package com.example.adminwebjava.service.payment;

import com.example.adminwebjava.entity.PaymentPlan;
import com.example.adminwebjava.entity.PaymentTransaction;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@UtilityClass
public class OwnerFeaturedPlanHelper {

    public static final String FEATURED_BADGE_TEXT = "Nổi bật";

    public static boolean isFeaturedMapPlan(PaymentPlan plan) {
        if (plan == null) {
            return false;
        }

        String planCode = plan.getPlanCode() != null ? plan.getPlanCode() : "";
        String planName = plan.getPlanName() != null ? plan.getPlanName() : "";
        String description = plan.getDescription() != null ? plan.getDescription() : "";

        String text = (planCode + " " + planName + " " + description).toUpperCase(Locale.ROOT);

        boolean matchesKeyword = text.contains("FEATURED")
                || text.contains("NOI_BAT")
                || text.contains("NỔI BẬT")
                || text.contains("NOI BAT")
                || text.contains("PREMIUM")
                || text.contains("PRO")
                || text.contains("PRIORITY")
                || text.contains("ƯU TIÊN")
                || text.contains("UU TIEN");

        boolean matchesPrice = plan.getPrice() != null && plan.getPrice().compareTo(new BigDecimal("199000")) >= 0;

        return matchesKeyword || matchesPrice;
    }

    public static boolean isPaymentStillActive(PaymentTransaction payment) {
        if (payment == null) {
            return false;
        }

        PaymentPlan plan = payment.getPaymentPlan();
        if (plan == null) {
            return false;
        }

        Instant paidAt = payment.getPaidAt() != null ? payment.getPaidAt() : payment.getCreatedAt();
        if (paidAt == null) {
            return false;
        }

        int durationDays = (plan.getDurationDays() == null || plan.getDurationDays() <= 0) ? 30 : plan.getDurationDays();
        Instant expiresAt = paidAt.plus(durationDays, ChronoUnit.DAYS);

        return expiresAt.isAfter(Instant.now());
    }

    public static String getFeaturedBadgeText() {
        return FEATURED_BADGE_TEXT;
    }
}