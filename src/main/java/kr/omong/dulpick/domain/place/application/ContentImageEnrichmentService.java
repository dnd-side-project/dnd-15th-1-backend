package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.ContentImageEnrichmentBacklog;
import kr.omong.dulpick.domain.place.domain.ContentImageEnrichmentBacklogRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ContentImageEnrichmentService {

    private static final Logger logger = LoggerFactory.getLogger(ContentImageEnrichmentService.class);
    private static final Duration RETRY_DELAY = Duration.ofMinutes(1);
    private static final Duration STALE_TASK_TIMEOUT = Duration.ofMinutes(10);
    private static final int RECOVERY_BATCH_SIZE = 20;
    private static final int MAX_RETRY_ATTEMPTS = 5;

    private final ContentImageStorageService storageService;
    private final ContentImageEnrichmentBacklogRepository backlogRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Executor executor;
    private final AtomicBoolean shuttingDown = new AtomicBoolean();

    @Autowired
    public ContentImageEnrichmentService(
            ContentImageStorageService storageService,
            ContentImageEnrichmentBacklogRepository backlogRepository,
            ObjectMapper objectMapper,
            Clock clock,
            @Qualifier("contentImageExecutor") Executor executor
    ) {
        this.storageService = storageService;
        this.backlogRepository = backlogRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.executor = executor;
    }

    public ContentImageEnrichmentService(
            ContentImageStorageService storageService,
            Executor executor
    ) {
        this.storageService = storageService;
        this.backlogRepository = null;
        this.objectMapper = null;
        this.clock = Clock.systemUTC();
        this.executor = executor;
    }

    public void dispatch(Long contentId, List<String> sourceUrls) {
        if (shuttingDown.get() || contentId == null || sourceUrls == null || sourceUrls.isEmpty()) {
            return;
        }
        List<String> imageUrls = List.copyOf(sourceUrls);
        Instant now = clock.instant();
        boolean persisted = enqueue(contentId, imageUrls, now, now);
        try {
            executor.execute(() -> store(contentId, imageUrls));
        } catch (RejectedExecutionException exception) {
            if (!persisted) {
                enqueue(contentId, imageUrls, now, now);
            }
            logger.warn(
                    "Content image enrichment deferred to recovery: contentId={}",
                    contentId
            );
        }
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void recoverPending() {
        if (shuttingDown.get() || backlogRepository == null) {
            return;
        }
        Instant now = clock.instant();
        backlogRepository.findRecoverable(
                        now,
                        now.minus(STALE_TASK_TIMEOUT),
                        PageRequest.of(0, RECOVERY_BATCH_SIZE)
                )
                .forEach(this::recover);
    }

    private void store(Long contentId, List<String> sourceUrls) {
        if (shuttingDown.get()) {
            return;
        }
        try {
            storageService.storeIfAvailable(contentId, sourceUrls);
            if (backlogRepository == null) {
                return;
            }
            if (storageService.hasAllStoredImages(contentId)) {
                backlogRepository.deleteByContentId(contentId);
            } else {
                scheduleRetry(contentId, sourceUrls);
            }
        } catch (RuntimeException exception) {
            if (shuttingDown.get() || Thread.currentThread().isInterrupted()) {
                return;
            }
            scheduleRetry(contentId, sourceUrls);
            logger.warn(
                    "Content image enrichment failed: contentId={}, cause={}",
                    contentId,
                    exception.getClass().getSimpleName()
            );
        }
    }

    private void recover(ContentImageEnrichmentBacklog backlog) {
        if (shuttingDown.get()) {
            return;
        }
        if (backlogRepository.claim(
                backlog.getContentId(),
                clock.instant().minus(STALE_TASK_TIMEOUT)
        ) == 0) {
            return;
        }
        try {
            List<String> sourceUrls = objectMapper.readValue(
                    backlog.getSourceUrls(),
                    new TypeReference<>() {
                    }
            );
            executor.execute(() -> store(backlog.getContentId(), sourceUrls));
        } catch (RejectedExecutionException exception) {
            scheduleRetry(backlog.getContentId());
        } catch (RuntimeException exception) {
            scheduleRetry(backlog.getContentId());
            logger.warn(
                    "Content image recovery failed: contentId={}, cause={}",
                    backlog.getContentId(),
                    exception.getClass().getSimpleName()
            );
        }
    }

    private boolean enqueue(
            Long contentId,
            List<String> sourceUrls,
            Instant nextAttemptAt,
            Instant now
    ) {
        if (backlogRepository == null) {
            return false;
        }
        try {
            backlogRepository.enqueue(
                    contentId,
                    objectMapper.writeValueAsString(sourceUrls),
                    nextAttemptAt,
                    now
            );
            return true;
        } catch (Exception exception) {
            logger.error("Content image recovery enqueue failed: contentId={}", contentId, exception);
            return false;
        }
    }

    private void scheduleRetry(Long contentId) {
        scheduleRetry(contentId, List.of());
    }

    private void scheduleRetry(Long contentId, List<String> sourceUrls) {
        if (shuttingDown.get() || backlogRepository == null) {
            return;
        }
        Instant now = clock.instant();
        if (!sourceUrls.isEmpty()) {
            enqueue(contentId, sourceUrls, now.plus(RETRY_DELAY), now);
        }
        backlogRepository.scheduleRetry(contentId, now.plus(RETRY_DELAY), now, MAX_RETRY_ATTEMPTS);
    }

    @PreDestroy
    void stopAcceptingTasks() {
        shuttingDown.set(true);
    }
}
