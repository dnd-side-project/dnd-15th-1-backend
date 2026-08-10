package kr.omong.dulpick.domain.place.application.scheduled;

import kr.omong.dulpick.domain.place.application.PlaceImportProcessingService;
import kr.omong.dulpick.domain.place.config.PlaceAnalysisProperties;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.concurrent.Executor;

@Service
@ConditionalOnProperty(
        name = "place-analysis.recovery-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PlaceImportRecoveryProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PlaceImportRecoveryProcessor.class);

    private final PlaceImportRepository importRepository;
    private final PlaceImportProcessingService processingService;
    private final PlaceAnalysisProperties properties;
    private final Clock clock;
    private final Executor executor;

    public PlaceImportRecoveryProcessor(
            PlaceImportRepository importRepository,
            PlaceImportProcessingService processingService,
            PlaceAnalysisProperties properties,
            Clock clock,
            @Qualifier("placeImportExecutor") Executor executor
    ) {
        this.importRepository = importRepository;
        this.processingService = processingService;
        this.properties = properties;
        this.clock = clock;
        this.executor = executor;
    }

    @Scheduled(
            initialDelayString = "${place-analysis.recovery-delay:5s}",
            fixedDelayString = "${place-analysis.recovery-delay:5s}"
    )
    public void process() {
        if (!properties.enabled()) {
            return;
        }
        importRepository.findRecoverableIds(
                clock.instant().minus(properties.recoveryDelay()),
                clock.instant().minusSeconds(properties.staleTimeoutSeconds()),
                PageRequest.of(0, properties.recoveryBatchSize())
        ).forEach(this::dispatchSafely);
    }

    private void dispatchSafely(Long importId) {
        try {
            String claimToken = processingService.claimPending(importId);
            if (claimToken != null) {
                executor.execute(() -> processSafely(importId, claimToken));
            }
        } catch (RuntimeException exception) {
            logger.error("Place import dispatch failed: importId={}", importId, exception);
        }
    }

    private void processSafely(Long importId, String claimToken) {
        try {
            processingService.processClaimed(importId, claimToken);
        } catch (RuntimeException exception) {
            logger.error("Place import processing failed: importId={}", importId, exception);
        }
    }
}
