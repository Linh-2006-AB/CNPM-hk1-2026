package com.example.adminwebjava.services.mediaprocessing;

import java.util.concurrent.CompletableFuture;

public interface IMultilingualMediaProcessor {

    CompletableFuture<MediaProcessingResult> processAsync(
            MediaProcessingRequest request
    );
}