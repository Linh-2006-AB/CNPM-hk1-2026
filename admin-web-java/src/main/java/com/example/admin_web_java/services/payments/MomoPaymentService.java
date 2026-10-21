package com.example.adminwebjava.service.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.adminwebjava.entity.PaymentTransaction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomoPaymentService {

    private static final String DEFAULT_ENDPOINT = "https://test-payment.momo.vn/v2/gateway/api/create";
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${momo.demo-mode:false}")
    private Boolean demoMode;

    @Value("${momo.partner-code:}")
    private String partnerCode;

    @Value("${momo.access-key:}")
    private String accessKey;

    @Value("${momo.secret-key:}")
    private String secretKey;

    @Value("${momo.endpoint:}")
    private String endpointConfig;

    public boolean isDemoMode() {
        return Boolean.TRUE.equals(demoMode);
    }

    public boolean isConfigured() {
        return isRealValue(partnerCode) &&
               isRealValue(accessKey) &&
               isRealValue(secretKey) &&
               isRealValue(getEndpoint());
    }

    public boolean canCreatePayment() {
        return isConfigured() || isDemoMode();
    }

    public String getEndpoint() {
        return (endpointConfig == null || endpointConfig.isBlank()) 
                ? DEFAULT_ENDPOINT 
                : endpointConfig.trim();
    }

    public MomoCreatePaymentResult createPayment(
            PaymentTransaction payment,
            String orderInfo,
            String redirectUrl,
            String ipnUrl) {

        ensureConfigured();

        // Round decimal away from zero (equivalent to AwayFromZero in .NET)
        BigDecimal amountDecimal = payment.getAmount().setScale(0, RoundingMode.HALF_UP);
        String amount = amountDecimal.toPlainString();

        String orderId = payment.getTransactionCode();
        String requestId = orderId + "-" + System.currentTimeMillis();
        String requestType = "captureWallet";
        String extraData = "";

        String rawSignature = String.format(
                "accessKey=%s&amount=%s&extraData=%s&ipnUrl=%s&orderId=%s&orderInfo=%s&partnerCode=%s&redirectUrl=%s&requestId=%s&requestType=%s",
                accessKey, amount, extraData, ipnUrl, orderId, orderInfo, partnerCode, redirectUrl, requestId, requestType
        );

        MomoCreatePaymentRequest payload = new MomoCreatePaymentRequest();
        payload.setPartnerCode(partnerCode);
        payload.setAccessKey(accessKey);
        payload.setRequestId(requestId);
        payload.setAmount(amount);
        payload.setOrderId(orderId);
        payload.setOrderInfo(orderInfo);
        payload.setRedirectUrl(redirectUrl);
        payload.setIpnUrl(ipnUrl);
        payload.setExtraData(extraData);
        payload.setRequestType(requestType);
        payload.setLang("vi");
        payload.setSignature(hmacSha256(secretKey, rawSignature));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity entity = new HttpEntity<>(payload, headers);

            ResponseEntity response = restTemplate.postForEntity(getEndpoint(), entity, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("MoMo HTTP Error: " + response.getStatusCode());
            }

            MomoCreatePaymentResponse result = objectMapper.readValue(response.getBody(), MomoCreatePaymentResponse.class);

            if (result == null || result.getResultCode() == null || result.getResultCode() != 0 || result.getPayUrl() == null || result.getPayUrl().isBlank()) {
                String message = (result != null && result.getMessage() != null) ? result.getMessage() : "Không tạo được link thanh toán MoMo.";
                throw new IllegalStateException("MoMo error: " + message);
            }

            return new MomoCreatePaymentResult(
                    result.getPayUrl(),
                    result.getDeeplink(),
                    result.getQrCodeUrl(),
                    result.getResultCode(),
                    result.getMessage(),
                    requestId
            );
        } catch (Exception e) {
            log.error("Failed to create MoMo payment", e);
            throw new IllegalStateException("MoMo payment creation error: " + e.getMessage(), e);
        }
    }

    public boolean verifySignature(Map params) {
        if (params == null || !params.containsKey("signature") || params.get("signature") == null || params.get("signature").isBlank()) {
            return false;
        }

        String signature = params.get("signature");
        String raw = buildReturnSignatureRaw(params);
        String expected = hmacSha256(secretKey, raw);

        return fixedEquals(expected, signature);
    }

    public boolean verifySignature(JsonNode json) {
        Map values = new HashMap<>();
        json.fieldNames().forEachRemaining(field -> {
            JsonNode node = json.get(field);
            values.put(field, node.isTextual() ? node.asText() : node.toString());
        });
        return verifySignature(values);
    }

    public boolean isSuccess(Map params) {
        return params != null && "0".equals(params.get("resultCode"));
    }

    public boolean isSuccess(JsonNode json) {
        if (json != null && json.has("resultCode")) {
            JsonNode code = json.get("resultCode");
            return code.isInt() ? code.asInt() == 0 : "0".equals(code.asText());
        }
        return false;
    }

    public String getOrderId(Map params) {
        return params != null ? params.getOrDefault("orderId", "") : "";
    }

    public String getOrderId(JsonNode json) {
        return (json != null && json.has("orderId")) ? json.get("orderId").asText("") : "";
    }

    public String getGatewayStatus(Map params) {
        return params != null ? params.getOrDefault("resultCode", "") : "";
    }

    public String getGatewayStatus(JsonNode json) {
        return (json != null && json.has("resultCode")) ? json.get("resultCode").asText("") : "";
    }

    public String getTransactionId(Map params) {
        return params != null ? params.get("transId") : null;
    }

    public String getTransactionId(JsonNode json) {
        return (json != null && json.has("transId")) ? json.get("transId").asText(null) : null;
    }

    private String buildReturnSignatureRaw(Map values) {
        java.util.function.Function getVal = key -> values.getOrDefault(key, "");
        return String.format(
                "accessKey=%s&amount=%s&extraData=%s&message=%s&orderId=%s&orderInfo=%s&orderType=%s&partnerCode=%s&payType=%s&requestId=%s&responseTime=%s&resultCode=%s&transId=%s",
                accessKey,
                getVal.apply("amount"),
                getVal.apply("extraData"),
                getVal.apply("message"),
                getVal.apply("orderId"),
                getVal.apply("orderInfo"),
                getVal.apply("orderType"),
                getVal.apply("partnerCode"),
                getVal.apply("payType"),
                getVal.apply("requestId"),
                getVal.apply("responseTime"),
                getVal.apply("resultCode"),
                getVal.apply("transId")
        );
    }

    private static String hmacSha256(String key, String input) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKeySpec);
            byte[] hash = hmac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).toLowerCase();
        } catch (Exception e) {
            throw new RuntimeException("Error calculating HMAC-SHA256", e);
        }
    }

    private static boolean fixedEquals(String left, String right) {
        if (left == null || right == null) return false;
        byte[] leftBytes = left.toLowerCase().getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(leftBytes, rightBytes);
    }

    private static boolean isRealValue(String value) {
        return value != null &&
               !value.isBlank() &&
               !value.toUpperCase().contains("CHANGE") &&
               !value.toUpperCase().contains("YOUR_");
    }

    private void ensureConfigured() {
        if (isDemoMode()) {
            throw new IllegalStateException("MoMo đang bật DemoMode. Không gọi API MoMo thật.");
        }
        if (!isConfigured()) {
            throw new IllegalStateException("Chưa cấu hình MoMo: momo.partner-code, momo.access-key, momo.secret-key và momo.endpoint.");
        }
    }

    // Inner Records and DTOs
    public record MomoCreatePaymentResult(
            String payUrl,
            String deeplink,
            String qrCodeUrl,
            Integer resultCode,
            String message,
            String requestId
    ) {}

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MomoCreatePaymentRequest {
        @JsonProperty("partnerCode")
        private String partnerCode;

        @JsonProperty("accessKey")
        private String accessKey;

        @JsonProperty("requestId")
        private String requestId;

        @JsonProperty("amount")
        private String amount;

        @JsonProperty("orderId")
        private String orderId;

        @JsonProperty("orderInfo")
        private String orderInfo;

        @JsonProperty("redirectUrl")
        private String redirectUrl;

        @JsonProperty("ipnUrl")
        private String ipnUrl;

        @JsonProperty("extraData")
        private String extraData;

        @JsonProperty("requestType")
        private String requestType;

        @JsonProperty("lang")
        private String lang = "vi";

        @JsonProperty("signature")
        private String signature;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MomoCreatePaymentResponse {
        @JsonProperty("partnerCode")
        private String partnerCode;

        @JsonProperty("orderId")
        private String orderId;

        @JsonProperty("requestId")
        private String requestId;

        @JsonProperty("amount")
        private Long amount;

        @JsonProperty("responseTime")
        private Long responseTime;

        @JsonProperty("message")
        private String message;

        @JsonProperty("resultCode")
        private Integer resultCode;

        @JsonProperty("payUrl")
        private String payUrl;

        @JsonProperty("deeplink")
        private String deeplink;

        @JsonProperty("qrCodeUrl")
        private String qrCodeUrl;
    }
}