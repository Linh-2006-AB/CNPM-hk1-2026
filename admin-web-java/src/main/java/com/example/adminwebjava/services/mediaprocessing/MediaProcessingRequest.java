package com.example.adminwebjava.services.mediaprocessing;

import java.util.UUID;

public record MediaProcessingRequest(
        UUID mediaTaskId,
        Integer poiId
) {}