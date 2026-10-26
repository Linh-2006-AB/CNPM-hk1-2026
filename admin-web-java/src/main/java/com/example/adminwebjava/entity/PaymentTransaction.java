package com.example.adminwebjava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "TransactionCode", nullable = false)
    private String transactionCode = "";

    @Column(name = "PayerType", nullable = false)
    private String payerType = "Owner";

    @Column(name = "OwnerProfileId")
    private Integer ownerProfileId;

    @Column(name = "TouristId")
    private Integer touristId;

    @Column(name = "PaymentPlanId")
    private Integer paymentPlanId;

    @Column(name = "Purpose", nullable = false)
    private String purpose = "Subscription";

    @Column(name = "Amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "Currency", nullable = false)
    private String currency = "VND";

    @Column(name = "PaymentMethod", nullable = false)
    private String paymentMethod = "Manual";

    @Column(name = "Status", nullable = false)
    private String status = "Pending";

    @Column(name = "Note")
    private String note;

    @Column(name = "CreatedAt", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "PaidAt")
    private Instant paidAt;

    @Column(name = "CheckoutUrl")
    private String checkoutUrl;

    @Column(name = "GatewayOrderCode")
    private String gatewayOrderCode;

    @Column(name = "GatewayPaymentLinkId")
    private String gatewayPaymentLinkId;

    @Column(name = "GatewayStatus")
    private String gatewayStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PaymentPlanId", insertable = false, updatable = false)
    private PaymentPlan paymentPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OwnerProfileId", insertable = false, updatable = false)
    private OwnerProfile ownerProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TouristId", insertable = false, updatable = false)
    private Tourist tourist;
}
