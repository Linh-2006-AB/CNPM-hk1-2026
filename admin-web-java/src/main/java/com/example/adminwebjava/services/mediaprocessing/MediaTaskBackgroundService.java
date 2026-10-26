package com.example.adminwebjava.services.mediaprocessing;

import com.example.adminwebjava.entity.MediaTask;
import com.example.adminwebjava.entity.MediaTaskStatus;
import com.example.adminwebjava.repository.MediaTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaTaskBackgroundService {

    private final MediaTaskRepository mediaTaskRepository;
    private final IMultilingualMediaProcessor multilingualMediaProcessor;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void recoverInterruptedTasks() {
        try {
            List<MediaTask> interruptedTasks = mediaTaskRepository.findByStatus(MediaTaskStatus.PROCESSING);

            for (MediaTask task : interruptedTasks) {
                task.setStatus(MediaTaskStatus.PENDING);
                task.setLastError("Ứng dụng đã dừng khi tác vụ đang xử lý. Tác vụ sẽ được chạy lại.");
                task.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
                task.setCompletedAt(null);
            }

            if (!interruptedTasks.isEmpty()) {
                mediaTaskRepository.saveAll(interruptedTasks);
                log.info("Recovered {} interrupted media tasks.", interruptedTasks.size());
            }
        } catch (Exception e) {
            log.error("Failed to recover interrupted tasks on startup", e);
        }
    }

    @Scheduled(fixedDelay = 2000)
    public void processNextTaskLoop() {
        try {
            MediaProcessingRequest request = claimNextTask();
            if (request == null) {
                return;
            }

            processRequest(request);
        } catch (Exception exception) {
            log.error("Media task worker loop failed.", exception);
        }
    }

    @Transactional
    public MediaProcessingRequest claimNextTask() {
        MediaTask task = mediaTaskRepository.findFirstByStatusOrderByCreatedAtAsc(MediaTaskStatus.PENDING)
                .orElse(null);

        if (task == null) {
            return null;
        }

        task.setStatus(MediaTaskStatus.PROCESSING);
        task.setProgressPercentage(0);
        task.setTotalLanguages(0);
        task.setSucceededLanguages(0);
        task.setFailedLanguages(0);
        task.setLastError(null);
        task.setAttemptCount(task.getAttemptCount() != null ? task.getAttemptCount() + 1 : 1);
        task.setStartedAt(LocalDateTime.now(ZoneOffset.UTC));
        task.setCompletedAt(null);
        task.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

        mediaTaskRepository.save(task);

        return new MediaProcessingRequest(task.getId(), task.getPoiId());
    }

    private void processRequest(MediaProcessingRequest request) {
        try {
            MediaProcessingResult result = multilingualMediaProcessor.processAsync(request).get();

            MediaTaskStatus status = (result.errors() == null || result.errors().isEmpty())
                    ? MediaTaskStatus.COMPLETED
                    : (result.succeededLanguages() > 0
                        ? MediaTaskStatus.COMPLETED_WITH_ERRORS
                        : MediaTaskStatus.FAILED);

            finishTask(
                    request.mediaTaskId(),
                    status,
                    result.totalLanguages(),
                    result.succeededLanguages(),
                    result.getFailedLanguages(),
                    joinErrors(result.errors())
            );
        } catch (Exception exception) {
            log.error("Background processing failed for MediaTask {}.", request.mediaTaskId(), exception);

            finishTask(
                    request.mediaTaskId(),
                    MediaTaskStatus.FAILED,
                    null,
                    null,
                    null,
                    exception.getMessage()
            );
        }
    }

    @Transactional
    public void finishTask(
            UUID mediaTaskId,
            MediaTaskStatus status,
            Integer totalLanguages,
            Integer succeededLanguages,
            Integer failedLanguages,
            String lastError) {

        MediaTask task = mediaTaskRepository.findById(mediaTaskId).orElse(null);

        if (task == null) {
            return;
        }

        task.setStatus(status);
        task.setProgressPercentage(100);
        if (totalLanguages != null) {
            task.setTotalLanguages(totalLanguages);
        }
        if (succeededLanguages != null) {
            task.setSucceededLanguages(succeededLanguages);
        }
        if (failedLanguages != null) {
            task.setFailedLanguages(failedLanguages);
        }
        task.setLastError(truncate(lastError, 4000));
        task.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));
        task.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

        mediaTaskRepository.save(task);
    }

    private static String joinErrors(List errors) {
        if (errors == null || errors.isEmpty()) {
            return null;
        }
        return truncate(String.join(System.lineSeparator(), errors), 4000);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isBlank() || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}