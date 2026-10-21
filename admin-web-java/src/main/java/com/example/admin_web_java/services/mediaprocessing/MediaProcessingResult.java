package com.example.adminwebjava.service.mediaprocessing;

import java.util.Collections;
import java.util.List;

public record MediaProcessingResult(
        int totalLanguages,
        int succeededLanguages,
        List errors
) {
    public int getFailedLanguages() {
        return errors != null ? errors.size() : 0;
    }

    public static final MediaProcessingResult EMPTY = 
            new MediaProcessingResult(0, 0, Collections.emptyList());
}