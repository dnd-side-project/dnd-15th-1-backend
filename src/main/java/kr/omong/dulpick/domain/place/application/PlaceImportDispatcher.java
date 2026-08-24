package kr.omong.dulpick.domain.place.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.Executor;

@Service
public class PlaceImportDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(PlaceImportDispatcher.class);

    private final PlaceImportProcessingService processingService;
    private final Executor executor;
    private final Clock clock;

    public PlaceImportDispatcher(
            PlaceImportProcessingService processingService,
            @Qualifier("placeImportExecutor") Executor executor,
            Clock clock
    ) {
        this.processingService = processingService;
        this.executor = executor;
        this.clock = clock;
    }

    public void dispatch(Long importId) {
        Instant queuedAt = clock.instant();
        try {
            executor.execute(() -> claimAndProcess(importId, queuedAt));
        } catch (RuntimeException exception) {
            logger.error("Place import dispatch failed: importId={}", importId, exception);
        }
    }

    private void claimAndProcess(Long importId, Instant queuedAt) {
        try {
            String claimToken = processingService.claimPending(importId);
            if (claimToken != null) {
                processSafely(importId, claimToken, queuedAt);
            }
        } catch (RuntimeException exception) {
            logger.error("Place import claim failed: importId={}", importId, exception);
        }
    }

    private void processSafely(Long importId, String claimToken, Instant queuedAt) {
        try {
            processingService.processClaimed(importId, claimToken, queuedAt);
        } catch (RuntimeException exception) {
            logger.error("Place import processing failed: importId={}", importId, exception);
        }
    }
}
