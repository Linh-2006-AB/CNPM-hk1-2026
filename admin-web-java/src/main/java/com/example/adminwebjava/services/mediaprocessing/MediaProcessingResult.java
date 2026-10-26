package com.example.adminwebjava.services.mediaprocessing;

import java.util.Collections;
import java.util.List;

public record MediaProcessingResult(
        int totalLanguages,
        int succeededLanguages,
        List<String> errors
) {
    public int getFailedLanguages() {
        return errors != null ? errors.size() : 0;
    }

    public static MediaProcessingResult empty() { return EMPTY; }

    public static final MediaProcessingResult EMPTY = 
            new MediaProcessingResult(0, 0, Collections.emptyList());
}