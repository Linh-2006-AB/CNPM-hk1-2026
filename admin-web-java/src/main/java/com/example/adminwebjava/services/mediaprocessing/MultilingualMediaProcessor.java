package com.example.adminwebjava.services.mediaprocessing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.adminwebjava.entity.MediaTask;
import com.example.adminwebjava.entity.PoiTranslation;
import com.example.adminwebjava.entity.SupportedLanguage;
import com.example.adminwebjava.entity.MediaTaskType;
import com.example.adminwebjava.repository.MediaTaskRepository;
import com.example.adminwebjava.repository.PoiTranslationRepository;
import com.example.adminwebjava.repository.SupportedLanguageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultilingualMediaProcessor implements IMultilingualMediaProcessor {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final MediaTaskRepository mediaTaskRepository;
    private final PoiTranslationRepository poiTranslationRepository;
    private final SupportedLanguageRepository supportedLanguageRepository;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    @Value("${openai.api-key:}")
    private String openAiApiKey;

    @Value("${openai.transcription-model:whisper-1}")
    private String openAiModel;

    @Value("${openai.transcription-endpoint:https://api.openai.com/v1/audio/transcriptions}")
    private String openAiEndpoint;

    @Value("${edge-tts.executable:edge-tts}")
    private String edgeTtsExecutable;

    @Value("${ffmpeg.executable:ffmpeg}")
    private String ffmpegExecutable;

    @Value("${web.root-path:wwwroot}")
    private String webRootPath;

    @Override
    @Async
    public CompletableFuture<MediaProcessingResult> processAsync(MediaProcessingRequest request) {
        try {
            TaskData taskData = loadTaskData(request);
            updateProgress(request.mediaTaskId(), 0, taskData.languages().size(), 0, 0, null);

            MediaProcessingResult result;
            if (taskData.taskType() == MediaTaskType.TEXT_TO_AUDIO) {
                result = processTextToAudio(request, taskData.source(), taskData.languages());
            } else if (taskData.taskType() == MediaTaskType.VIDEO_DUBBING) {
                result = processVideoDubbing(request, taskData.source(), taskData.languages(), taskData.sourceAudioPath(), taskData.sourceVideoPath());
            } else {
                throw new IllegalArgumentException("Unsupported media task type: " + taskData.taskType());
            }

            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Error processing MediaTask {}", request.mediaTaskId(), e);
            CompletableFuture future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    private MediaProcessingResult processTextToAudio(
            MediaProcessingRequest request,
            SourceTranslation sourceTranslation,
            List<SupportedLanguage> languages) {

        if (languages.isEmpty()) {
            updateProgress(request.mediaTaskId(), 100, 0, 0, 0, null);
            return MediaProcessingResult.empty();
        }

        int attemptedLanguages = 0;
        int successfulLanguages = 0;
        List<String> errors = new ArrayList<>();

        for (SupportedLanguage language : languages) {
            try {
                PoiTranslation translation = poiTranslationRepository
                        .findByPoiIdAndLanguageCode(request.poiId(), language.getLanguageCode())
                        .orElse(null);

                boolean isVietnamese = "vi".equalsIgnoreCase(language.getLanguageCode());
                TranslatedContent translatedContent;

                if (isVietnamese) {
                    translatedContent = new TranslatedContent(
                            sourceTranslation.name(),
                            sourceTranslation.shortDescription(),
                            sourceTranslation.fullDescription()
                    );
                } else if (translation != null && isNotBlank(translation.getName()) && isNotBlank(translation.getFullDescription())) {
                    translatedContent = new TranslatedContent(
                            translation.getName(),
                            translation.getShortDescription() != null ? translation.getShortDescription() : "",
                            translation.getFullDescription()
                    );
                } else {
                    translatedContent = translateWithGemini(sourceTranslation, language);
                }

                if (translation == null) {
                    translation = new PoiTranslation();
                    translation.setPoiId(request.poiId());
                    translation.setLanguageCode(language.getLanguageCode());
                }

                translation.setName(translatedContent.name());
                translation.setShortDescription(translatedContent.shortDescription());
                translation.setFullDescription(translatedContent.fullDescription());
                translation.setTtsScript(translatedContent.fullDescription());
                translation.setAudioUrl(null);
                translation.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

                poiTranslationRepository.save(translation);

                if (!isNotBlank(language.getEdgeTtsVoice())) {
                    successfulLanguages++;
                    log.info("Saved text translation for POI {}, lang {}. Edge-TTS voice empty, skipped audio.", request.poiId(), language.getLanguageCode());
                    continue;
                }

                GeneratedMediaFile generatedAudio = generateAudio(
                        request.poiId(),
                        language,
                        translatedContent.fullDescription()
                );

                translation.setAudioUrl(generatedAudio.webUrl());
                translation.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
                poiTranslationRepository.save(translation);

                successfulLanguages++;
                log.info("Generated translation and audio for POI {}, lang {}.", request.poiId(), language.getLanguageCode());

            } catch (Exception exception) {
                String errorMsg = language.getLanguageCode() + ": " + exception.getMessage();
                errors.add(errorMsg);
                log.error("Could not process POI {} in lang {}", request.poiId(), language.getLanguageCode(), exception);
            } finally {
                attemptedLanguages++;
                int progress = (int) Math.floor(attemptedLanguages * 100.0 / languages.size());
                String lastErr = errors.isEmpty() ? null : errors.get(errors.size() - 1);
                updateProgress(request.mediaTaskId(), progress, languages.size(), successfulLanguages, errors.size(), lastErr);
            }
        }

        return new MediaProcessingResult(languages.size(), successfulLanguages, errors);
    }

    private MediaProcessingResult processVideoDubbing(
            MediaProcessingRequest request,
            SourceTranslation sourceTranslation,
            List<SupportedLanguage> languages,
            String sourceAudioPath,
            String sourceVideoPath) throws IOException {

        if (!isNotBlank(sourceVideoPath) || !new File(sourceVideoPath).exists()) {
            throw new IllegalStateException("Source video for MediaTask " + request.mediaTaskId() + " does not exist.");
        }

        if (languages.isEmpty()) {
            updateProgress(request.mediaTaskId(), 100, 0, 0, 0, null);
            return MediaProcessingResult.empty();
        }

        Path workingDir = Paths.get(System.getProperty("java.io.tmpdir"), "TourGuideSystem", request.mediaTaskId().toString().replace("-", ""));
        Files.createDirectories(workingDir);

        try {
            String extractedWavPath = workingDir.resolve("source-vi.wav").toString();
            String inputForAudio = (isNotBlank(sourceAudioPath) && new File(sourceAudioPath).exists()) ? sourceAudioPath : sourceVideoPath;
            extractAudioFromVideo(inputForAudio, extractedWavPath);

            String vietnameseTranscript = transcribeVietnamese(extractedWavPath);

            int attemptedLanguages = 0;
            int successfulLanguages = 0;
            List<String> errors = new ArrayList<>();

            for (SupportedLanguage language : languages) {
                try {
                    TranslatedVideoContent translatedContent = translateVideoContentWithGemini(
                            sourceTranslation,
                            vietnameseTranscript,
                            language
                    );

                    PoiTranslation translation = poiTranslationRepository
                            .findByPoiIdAndLanguageCode(request.poiId(), language.getLanguageCode())
                            .orElse(null);

                    if (translation == null) {
                        translation = new PoiTranslation();
                        translation.setPoiId(request.poiId());
                        translation.setLanguageCode(language.getLanguageCode());
                    }

                    translation.setName(translatedContent.name());
                    translation.setShortDescription(translatedContent.shortDescription());
                    translation.setFullDescription(translatedContent.fullDescription());
                    translation.setTtsScript(translatedContent.narration());
                    translation.setAudioUrl(null);
                    translation.setVideoUrl(null);
                    translation.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
                    poiTranslationRepository.save(translation);

                    if (!isNotBlank(language.getEdgeTtsVoice())) {
                        successfulLanguages++;
                        log.info("Saved video narration for POI {}, lang {}. Edge-TTS empty, skipped dubbing.", request.poiId(), language.getLanguageCode());
                        continue;
                    }

                    GeneratedMediaFile generatedAudio = generateAudio(
                            request.poiId(),
                            language,
                            translatedContent.narration()
                    );

                    translation.setAudioUrl(generatedAudio.webUrl());
                    translation.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
                    poiTranslationRepository.save(translation);

                    GeneratedMediaFile generatedVideo = mixDubbedAudioIntoVideo(
                            request.poiId(),
                            language,
                            sourceVideoPath,
                            generatedAudio.physicalPath()
                    );

                    translation.setVideoUrl(generatedVideo.webUrl());
                    translation.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
                    poiTranslationRepository.save(translation);

                    successfulLanguages++;
                    log.info("Generated dubbed video for POI {}, lang {}.", request.poiId(), language.getLanguageCode());

                } catch (Exception exception) {
                    String errorMsg = language.getLanguageCode() + ": " + exception.getMessage();
                    errors.add(errorMsg);
                    log.error("Could not dub POI {} in lang {}", request.poiId(), language.getLanguageCode(), exception);
                } finally {
                    attemptedLanguages++;
                    int progress = (int) Math.floor(attemptedLanguages * 100.0 / languages.size());
                    String lastErr = errors.isEmpty() ? null : errors.get(errors.size() - 1);
                    updateProgress(request.mediaTaskId(), progress, languages.size(), successfulLanguages, errors.size(), lastErr);
                }
            }

            return new MediaProcessingResult(languages.size(), successfulLanguages, errors);
        } finally {
            tryDeleteDirectory(workingDir.toFile());
        }
    }

    private TaskData loadTaskData(MediaProcessingRequest request) {
        MediaTask mediaTask = mediaTaskRepository.findById(request.mediaTaskId())
                .orElseThrow(() -> new IllegalStateException("MediaTask " + request.mediaTaskId() + " does not exist."));

        if (!mediaTask.getPoiId().equals(request.poiId())) {
            throw new IllegalStateException("MediaTask PoiId does not match the queued request.");
        }

        PoiTranslation viSource = poiTranslationRepository.findByPoiIdAndLanguageCode(request.poiId(), "vi")
                .orElseThrow(() -> new IllegalStateException("Vietnamese source translation for POI " + request.poiId() + " does not exist."));

        if (!isNotBlank(viSource.getFullDescription())) {
            throw new IllegalStateException("Vietnamese FullDescription for POI " + request.poiId() + " is empty.");
        }

        SourceTranslation source = new SourceTranslation(
                viSource.getName(),
                viSource.getShortDescription() != null ? viSource.getShortDescription() : "",
                viSource.getFullDescription(),
                viSource.getAudioUrl(),
                viSource.getVideoUrl()
        );

        List<SupportedLanguage> languages = supportedLanguageRepository.findByIsActiveTrueOrderByLanguageCodeAsc();

        return new TaskData(
                mediaTask.getTaskType(),
                source,
                languages,
                resolveLocalMediaPath(source.audioUrl()),
                resolveLocalMediaPath(source.videoUrl())
        );
    }

    private TranslatedContent translateWithGemini(SourceTranslation source, SupportedLanguage language) {
        if (!isNotBlank(geminiApiKey)) throw new IllegalStateException("Gemini API Key is not configured.");

        String cleanModel = geminiModel.toLowerCase().startsWith("models/") ? geminiModel.substring(7) : geminiModel;

        Map sourceMap = Map.of(
                "name", source.name(),
                "shortDescription", source.shortDescription(),
                "fullDescription", source.fullDescription()
        );

        String prompt = String.format("""
             Translate the following Vietnamese POI content into %s (language code: %s).

             Rules:
             - Preserve the original meaning and factual information.
             - Use natural language suitable for a tourism audio guide.
             - Translate the "name" field into the target language too.
             - Do not add explanations or new facts.
             - Return only one JSON object with exactly these string fields: "name", "shortDescription", "fullDescription".

             Vietnamese source JSON:
             %s
             """, language.getLanguageName(), language.getLanguageCode(), toJson(sourceMap));

        String responseBody = callGeminiApi(cleanModel, prompt);
        return parseGeminiTranslation(responseBody, TranslatedContent.class);
    }

    private TranslatedVideoContent translateVideoContentWithGemini(SourceTranslation source, String transcript, SupportedLanguage language) {
        if (!isNotBlank(geminiApiKey)) throw new IllegalStateException("Gemini API Key is not configured.");

        String cleanModel = geminiModel.toLowerCase().startsWith("models/") ? geminiModel.substring(7) : geminiModel;

        Map sourceMap = Map.of(
                "name", source.name(),
                "shortDescription", source.shortDescription(),
                "fullDescription", source.fullDescription(),
                "narration", transcript
        );

        String prompt = String.format("""
             Translate the following Vietnamese POI metadata and video narration into %s (language code: %s).

             Rules:
             - Preserve all factual information and the original meaning.
             - Write natural narration suitable for a tourism dubbed video.
             - Translate the "name" field into the target language too.
             - Do not add explanations or new facts.
             - Return only one JSON object with exactly these string fields: "name", "shortDescription", "fullDescription", "narration".

             Vietnamese source JSON:
             %s
             """, language.getLanguageName(), language.getLanguageCode(), toJson(sourceMap));

        String responseBody = callGeminiApi(cleanModel, prompt);
        return parseGeminiTranslation(responseBody, TranslatedVideoContent.class);
    }

    private String callGeminiApi(String model, String prompt) {
        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s", model, geminiApiKey);

        Map payload = Map.of(
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.2, "responseMimeType", "application/json")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpEntity entity = new HttpEntity<>(toJson(payload), headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return response.getBody();
                }
            } catch (Exception e) {
                if (attempt == maxAttempts) throw new IllegalStateException("Gemini API call failed: " + e.getMessage(), e);
                try { Thread.sleep(attempt * 2000L); } catch (InterruptedException ignored) {}
            }
        }
        throw new IllegalStateException("Gemini request failed unexpectedly.");
    }

    private <T> T parseGeminiTranslation(String responseBody, Class<T> clazz) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String candidateText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            if (!isNotBlank(candidateText)) throw new IllegalStateException("Gemini returned empty text.");

            String json = extractFirstJsonObject(candidateText);
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse Gemini response: " + e.getMessage(), e);
        }
    }

    private void extractAudioFromVideo(String inputPath, String outputWavPath) {
        runProcess(List.of(
                resolveFfmpegExecutable(),
                "-y", "-i", inputPath, "-vn", "-acodec", "pcm_s16le", "-ar", "16000", "-ac", "1", outputWavPath
        ), "FFmpeg audio extraction");
        ensureOutputFileExists(outputWavPath, "FFmpeg did not create the extracted WAV file.");
    }

    private String transcribeVietnamese(String wavPath) throws IOException {
        File wavFile = new File(wavPath);
        long safeFileSize = 24L * 1024 * 1024;

        if (wavFile.length() <= safeFileSize) {
            return transcribeAudioChunk(wavPath);
        }

        Path chunkDir = wavFile.getParentFile().toPath().resolve("transcription-chunks");
        Files.createDirectories(chunkDir);
        String outputPattern = chunkDir.resolve("chunk-%03d.wav").toString();

        runProcess(List.of(
                resolveFfmpegExecutable(),
                "-y", "-i", wavPath, "-f", "segment", "-segment_time", "600", "-reset_timestamps", "1", "-c", "copy", outputPattern
        ), "FFmpeg transcription audio splitting");

        File[] chunks = chunkDir.toFile().listFiles((dir, name) -> name.startsWith("chunk-") && name.endsWith(".wav"));
        if (chunks == null || chunks.length == 0) throw new IllegalStateException("FFmpeg did not create chunks.");

        Arrays.sort(chunks, Comparator.comparing(File::getName));
        List transcripts = new ArrayList<>();

        for (File chunk : chunks) {
            if (chunk.length() > safeFileSize) {
                throw new IllegalStateException("Chunk " + chunk.getName() + " is larger than 24MB.");
            }
            transcripts.add(transcribeAudioChunk(chunk.getAbsolutePath()));
        }

        return String.join("\n", transcripts);
    }

    private String transcribeAudioChunk(String audioPath) {
        if (!isNotBlank(openAiApiKey)) throw new IllegalStateException("OpenAI API Key is not configured.");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(openAiApiKey);

        MultiValueMap body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(audioPath));
        body.add("model", openAiModel);
        body.add("language", "vi");
        body.add("response_format", "json");

       HttpEntity<MultiValueMap<String, Object>> requestEntity =
        new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(openAiEndpoint, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String text = jsonNode.path("text").asText();
                if (!isNotBlank(text)) throw new IllegalStateException("OpenAI returned empty transcription.");
                return text.trim();
            }
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI transcription API error: " + e.getMessage(), e);
        }

        throw new IllegalStateException("OpenAI transcription failed.");
    }

    private GeneratedMediaFile generateAudio(Integer poiId, SupportedLanguage language, String text) throws IOException {
        if (!isNotBlank(language.getEdgeTtsVoice())) {
            throw new IllegalStateException("EdgeTtsVoice is empty for language " + language.getLanguageCode());
        }

        Path audioDir = Paths.get(webRootPath, "audio");
        Files.createDirectories(audioDir);

        String sanitizeCode = sanitizeFileNamePart(language.getLanguageCode());
        String outputFileName = String.format("poi-%d-%s-%s.mp3", poiId, sanitizeCode, UUID.randomUUID().toString().replace("-", ""));
        Path outputPath = audioDir.resolve(outputFileName);

        Path tempTextFile = Files.createTempFile("tour-guide-tts-", ".txt");

        try {
            Files.writeString(tempTextFile, text, StandardCharsets.UTF_8);

            List command = List.of(
                    resolveEdgeTtsExecutable(),
                    "--voice", language.getEdgeTtsVoice(),
                    "--file", tempTextFile.toAbsolutePath().toString(),
                    "--write-media", outputPath.toAbsolutePath().toString()
            );

            runProcess(command, "Edge-TTS audio generation");
            ensureOutputFileExists(outputPath.toString(), "edge-tts did not create a valid MP3 file.");

        } finally {
            tryDeleteFile(tempTextFile.toFile());
        }

        return new GeneratedMediaFile(outputPath.toAbsolutePath().toString(), "/audio/" + outputFileName);
    }

    private GeneratedMediaFile mixDubbedAudioIntoVideo(Integer poiId, SupportedLanguage language, String sourceVideoPath, String dubbedAudioPath) throws IOException {
        Path videoDir = Paths.get(webRootPath, "video");
        Files.createDirectories(videoDir);

        String sanitizeCode = sanitizeFileNamePart(language.getLanguageCode());
        String outputFileName = String.format("poi-%d-%s-%s.mp4", poiId, sanitizeCode, UUID.randomUUID().toString().replace("-", ""));
        Path outputPath = videoDir.resolve(outputFileName);

        boolean hasAudio = hasAudioStream(sourceVideoPath);

        List command;
        if (hasAudio) {
            command = List.of(
                    resolveFfmpegExecutable(),
                    "-y", "-i", sourceVideoPath, "-i", dubbedAudioPath,
                    "-filter_complex", "[0:a:0]volume=0.15[background];[background][1:a:0]amix=inputs=2:duration=first:dropout_transition=2:normalize=0[mixed]",
                    "-map", "0:v:0", "-map", "[mixed]",
                    "-c:v", "copy", "-c:a", "aac", "-b:a", "192k", "-movflags", "+faststart",
                    outputPath.toAbsolutePath().toString()
            );
        } else {
            command = List.of(
                    resolveFfmpegExecutable(),
                    "-y", "-i", sourceVideoPath, "-i", dubbedAudioPath,
                    "-filter_complex", "[1:a:0]apad[dub]",
                    "-map", "0:v:0", "-map", "[dub]",
                    "-c:v", "copy", "-c:a", "aac", "-b:a", "192k", "-shortest", "-movflags", "+faststart",
                    outputPath.toAbsolutePath().toString()
            );
        }

        runProcess(command, "FFmpeg video dubbing");
        ensureOutputFileExists(outputPath.toString(), "FFmpeg did not create the dubbed video.");

        return new GeneratedMediaFile(outputPath.toAbsolutePath().toString(), "/video/" + outputFileName);
    }

    private boolean hasAudioStream(String sourceVideoPath) {
        String output = runProcessForOutput(List.of(
                resolveFfprobeExecutable(),
                "-v", "error", "-select_streams", "a:0", "-show_entries", "stream=index", "-of", "csv=p=0", sourceVideoPath
        ), "FFprobe audio stream detection");

        return isNotBlank(output);
    }

    private String resolveEdgeTtsExecutable() {
        return isNotBlank(edgeTtsExecutable) ? edgeTtsExecutable : "edge-tts";
    }

    private String resolveFfmpegExecutable() {
        return isNotBlank(ffmpegExecutable) ? ffmpegExecutable : "ffmpeg";
    }

    private String resolveFfprobeExecutable() {
        String ffmpeg = resolveFfmpegExecutable();
        File ffmpegFile = new File(ffmpeg);
        if (ffmpegFile.getParent() != null) {
            String ffprobeName = System.getProperty("os.name").toLowerCase().contains("win") ? "ffprobe.exe" : "ffprobe";
            File ffprobe = new File(ffmpegFile.getParent(), ffprobeName);
            if (ffprobe.exists()) return ffprobe.getAbsolutePath();
        }
        return "ffprobe";
    }

    private static void runProcess(List command, String operation) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String errorLog = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalStateException(operation + " failed with exit code " + exitCode + ": " + errorLog);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute " + operation + ": " + e.getMessage(), e);
        }
    }

    private static String runProcessForOutput(List command, String operation) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            process.waitFor();
            return output.trim();
        } catch (Exception e) {
            log.warn("Failed process output check for {}: {}", operation, e.getMessage());
            return "";
        }
    }

    @Transactional
    public void updateProgress(UUID mediaTaskId, int progress, Integer totalLanguages, Integer succeededLanguages, Integer failedLanguages, String lastError) {
        MediaTask task = mediaTaskRepository.findById(mediaTaskId)
                .orElseThrow(() -> new IllegalStateException("MediaTask " + mediaTaskId + " does not exist."));

        task.setProgressPercentage(Math.max(0, Math.min(100, progress)));
        if (totalLanguages != null) task.setTotalLanguages(totalLanguages);
        if (succeededLanguages != null) task.setSucceededLanguages(succeededLanguages);
        if (failedLanguages != null) task.setFailedLanguages(failedLanguages);

        if (isNotBlank(lastError)) {
            task.setLastError(lastError.length() <= 4000 ? lastError : lastError.substring(0, 4000));
        }

        task.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        mediaTaskRepository.save(task);
    }

    private String resolveLocalMediaPath(String webUrl) {
        if (!isNotBlank(webUrl) || webUrl.startsWith("http://") || webUrl.startsWith("https://")) {
            return null;
        }

        Path root = Paths.get(webRootPath).toAbsolutePath().normalize();
        String relative = webUrl.startsWith("/") ? webUrl.substring(1) : webUrl;
        Path physical = root.resolve(relative).normalize();

        return physical.startsWith(root) ? physical.toString() : null;
    }

    private static void ensureOutputFileExists(String path, String errorMsg) {
        File file = new File(path);
        if (!file.exists() || file.length() == 0) {
            throw new IllegalStateException(errorMsg);
        }
    }

    private void tryDeleteDirectory(File dir) {
        if (dir != null && dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) tryDeleteDirectory(f);
                    else f.delete();
                }
            }
            dir.delete();
        }
    }

    private void tryDeleteFile(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    private String extractFirstJsonObject(String value) {
        int start = value.indexOf('{');
        if (start < 0) throw new IllegalStateException("No JSON object found.");

        int depth = 0;
        boolean insideString = false;
        boolean escaped = false;

        for (int i = start; i < value.length(); i++) {
            char c = value.charAt(i);

            if (insideString) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') insideString = false;
                continue;
            }

            if (c == '"') insideString = true;
            else if (c == '{') depth++;
            else if (c == '}' && --depth == 0) {
                return value.substring(start, i + 1);
            }
        }
        throw new IllegalStateException("Incomplete JSON object.");
    }

    private static String sanitizeFileNamePart(String value) {
        if (value == null) return "unknown";
        String sanitized = value.replaceAll("[^a-zA-Z0-9-_]", "");
        return sanitized.isBlank() ? "unknown" : sanitized;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialization error", e);
        }
    }

    private static boolean isNotBlank(String str) {
        return str != null && !str.isBlank();
    }

    // Inner record / DTO classes
    private record SourceTranslation(String name, String shortDescription, String fullDescription, String audioUrl, String videoUrl) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TranslatedContent(
            @JsonProperty("name") String name,
            @JsonProperty("shortDescription") String shortDescription,
            @JsonProperty("fullDescription") String fullDescription
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TranslatedVideoContent(
            @JsonProperty("name") String name,
            @JsonProperty("shortDescription") String shortDescription,
            @JsonProperty("fullDescription") String fullDescription,
            @JsonProperty("narration") String narration
    ) {}

    private record GeneratedMediaFile(String physicalPath, String webUrl) {}

    private record TaskData(MediaTaskType taskType, SourceTranslation source, List<SupportedLanguage> languages, String sourceAudioPath, String sourceVideoPath) {}
}