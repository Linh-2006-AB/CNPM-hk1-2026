package com.adminweb.services;

import java.util.concurrent.CompletableFuture;

public interface GeminiService {
    CompletableFuture optimizePoiContentAsync(String rawText);
    CompletableFuture generateTextAsync(String prompt);
}
