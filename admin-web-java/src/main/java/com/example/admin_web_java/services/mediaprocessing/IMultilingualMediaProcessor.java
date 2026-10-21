package com.example.adminwebjava.service.mediaprocessing;

import java.util.concurrent.CompletableFuture;

public interface IMultilingualMediaProcessor {

    CompletableFuture processAsync(
            MediaProcessingRequest request
    );
}