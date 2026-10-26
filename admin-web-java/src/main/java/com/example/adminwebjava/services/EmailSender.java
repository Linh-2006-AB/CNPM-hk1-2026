package com.example.adminwebjava.services;

import java.util.concurrent.CompletableFuture;

public interface EmailSender {
    boolean isConfigured();
    
    CompletableFuture sendAsync(String to, String subject, String htmlBody);
}