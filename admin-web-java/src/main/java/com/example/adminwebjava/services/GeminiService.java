package com.example.adminwebjava.services;

import java.util.concurrent.CompletableFuture;

public interface GeminiService {
    CompletableFuture<String> optimizePoiContentAsync(String rawText);
    CompletableFuture<String> generateTextAsync(String prompt);
}
