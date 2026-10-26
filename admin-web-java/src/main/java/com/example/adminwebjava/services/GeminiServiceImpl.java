package com.example.adminwebjava.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Google Gemini implementation used by content translation features.
 * The service is created even when no API key is configured so the web
 * application can start; Gemini operations then return a clear error.
 */
@Service
public class GeminiServiceImpl implements GeminiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String model;

    @Override
    public CompletableFuture<String> optimizePoiContentAsync(String rawText) {
        String prompt = "Bạn là biên tập viên nội dung du lịch. Hãy chỉnh sửa nội dung sau cho rõ ràng, "
                + "tự nhiên và phù hợp làm lời thuyết minh du lịch. Không bịa thêm dữ kiện. "
                + "Chỉ trả về nội dung đã chỉnh sửa, không giải thích.\n\nNội dung:\n" + rawText;
        return generateTextAsync(prompt);
    }

    @Override
    public CompletableFuture<String> generateTextAsync(String prompt) {
        return CompletableFuture.supplyAsync(() -> generateText(prompt));
    }

    private String generateText(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Chưa cấu hình Gemini API key. Hãy đặt thuộc tính gemini.api-key trong application.properties "
                            + "hoặc biến môi trường tương ứng trước khi dùng tính năng dịch bằng Gemini.");
        }

        String cleanModel = (model == null || model.isBlank()) ? "gemini-1.5-flash" : model.trim();
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + cleanModel
                + ":generateContent?key=" + apiKey.trim();

        Map<String, Object> payload = Map.of(
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.2)
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(requestBody, headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Gemini API trả về phản hồi rỗng hoặc trạng thái không thành công.");
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new IllegalStateException("Gemini không trả về nội dung văn bản.");
            }
            String text = parts.get(0).path("text").asText("");
            if (text.isBlank()) {
                throw new IllegalStateException("Gemini trả về văn bản rỗng.");
            }
            return text;
        } catch (Exception e) {
            if (e instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("Không thể gọi Gemini API: " + e.getMessage(), e);
        }
    }
}
