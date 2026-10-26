package com.example.adminwebjava.service.payment;

import com.example.adminwebjava.entity.PaymentTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VnPayPaymentService {

    private static final String DEFAULT_PAYMENT_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Value("${vnpay.tmn-code:}")
    private String tmnCode;

    @Value("${vnpay.hash-secret:}")
    private String hashSecret;

    @Value("${vnpay.payment-url:}")
    private String paymentUrlConfig;

    public boolean isConfigured() {
        return isRealValue(tmnCode) &&
               isRealValue(hashSecret) &&
               isRealValue(getPaymentUrl());
    }

    public String getPaymentUrl() {
        return (paymentUrlConfig == null || paymentUrlConfig.isBlank()) 
                ? DEFAULT_PAYMENT_URL 
                : paymentUrlConfig.trim();
    }

    public String createPaymentUrl(PaymentTransaction payment, String returnUrl, String ipAddress) {
        ensureConfigured();

        // Round decimal away from zero and multiply by 100 for VNPay amount format
        BigDecimal roundedAmount = payment.getAmount().setScale(0, RoundingMode.HALF_UP);
        long amount = roundedAmount.longValue() * 100L;

        String createDate = LocalDateTime.now().format(DATE_FORMATTER);
        String txnRef = normalizeTxnRef(payment.getTransactionCode());
        String clientIp = (ipAddress == null || ipAddress.isBlank()) ? "127.0.0.1" : ipAddress;

        String note = payment.getNote() != null ? payment.getNote() : ("VERSA " + txnRef);
        String orderInfo = normalizeOrderInfo(note);

        SortedMap<String, String> parameters = new TreeMap<>();
        parameters.put("vnp_Version", "2.1.0");
        parameters.put("vnp_Command", "pay");
        parameters.put("vnp_TmnCode", tmnCode);
        parameters.put("vnp_Amount", String.valueOf(amount));
        parameters.put("vnp_CreateDate", createDate);
        parameters.put("vnp_CurrCode", "VND");
        parameters.put("vnp_IpAddr", clientIp);
        parameters.put("vnp_Locale", "vn");
        parameters.put("vnp_OrderInfo", orderInfo);
        parameters.put("vnp_OrderType", "other");
        parameters.put("vnp_ReturnUrl", returnUrl);
        parameters.put("vnp_TxnRef", txnRef);

        String query = buildQuery(parameters);
        String secureHash = hmacSha512(hashSecret, query);

        return getPaymentUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    public boolean verifySignature(Map<String, String> params) {
        if (params == null || !params.containsKey("vnp_SecureHash")) {
            return false;
        }

        String providedHash = params.get("vnp_SecureHash");
        if (providedHash == null || providedHash.isBlank() || !isRealValue(hashSecret)) {
            return false;
        }

        SortedMap<String, String> values = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.toLowerCase().startsWith("vnp_") &&
                !"vnp_SecureHash".equalsIgnoreCase(key) &&
                !"vnp_SecureHashType".equalsIgnoreCase(key)) {
                values.put(key, entry.getValue());
            }
        }

        String raw = buildQuery(values);
        String expected = hmacSha512(hashSecret, raw);

        return fixedEquals(expected, providedHash);
    }

    public boolean isSuccess(Map<String, String> params) {
        if (params == null) return false;
        return "00".equalsIgnoreCase(params.get("vnp_ResponseCode")) &&
               "00".equalsIgnoreCase(params.get("vnp_TransactionStatus"));
    }

    public String getTxnRef(Map<String, String> params) {
        return params != null ? params.getOrDefault("vnp_TxnRef", "") : "";
    }

    public String getGatewayStatus(Map<String, String> params) {
        return params != null ? params.getOrDefault("vnp_ResponseCode", "") : "";
    }

    public String getTransactionNo(Map<String, String> params) {
        return params != null ? params.get("vnp_TransactionNo") : null;
    }

    private static String buildQuery(SortedMap<String, String> values) {
        return values.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String hmacSha512(String key, String input) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKeySpec);
            byte[] hash = hmac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).toLowerCase();
        } catch (Exception e) {
            throw new RuntimeException("Error calculating HMAC-SHA512", e);
        }
    }

    private static boolean fixedEquals(String left, String right) {
        if (left == null || right == null) return false;
        byte[] leftBytes = left.toLowerCase().getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(leftBytes, rightBytes);
    }

    private static String normalizeTxnRef(String value) {
        if (value == null) value = "";
        String cleaned = value.replaceAll("[^a-zA-Z0-9-_]", "");

        if (cleaned.isBlank()) {
            cleaned = String.valueOf(System.currentTimeMillis());
        }

        return cleaned.length() <= 100 ? cleaned : cleaned.substring(0, 100);
    }

    private static String normalizeOrderInfo(String value) {
        String input = (value == null || value.isBlank()) ? "VERSA Payment" : value;
        String cleaned = Arrays.stream(input.split("\\s+"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(" "));

        return cleaned.length() <= 180 ? cleaned : cleaned.substring(0, 180);
    }

    private static boolean isRealValue(String value) {
        return value != null &&
               !value.isBlank() &&
               !value.toUpperCase().contains("CHANGE") &&
               !value.toUpperCase().contains("YOUR_");
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException("Chưa cấu hình VNPay: vnpay.tmn-code, vnpay.hash-secret và vnpay.payment-url.");
        }
    }
}
