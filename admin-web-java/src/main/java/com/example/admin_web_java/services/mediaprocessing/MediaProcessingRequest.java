package com.example.adminwebjava.service.mediaprocessing;

import java.util.UUID;

public record MediaProcessingRequest(
        UUID mediaTaskId,
        Integer poiId
) {}