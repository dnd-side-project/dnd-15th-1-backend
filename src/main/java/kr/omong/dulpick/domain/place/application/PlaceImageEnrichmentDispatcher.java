package kr.omong.dulpick.domain.place.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Service
public class PlaceImageEnrichmentDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(PlaceImageEnrichmentDispatcher.class);

    private final PlaceImageEnrichmentService enrichmentService;
    private final Executor executor;

    public PlaceImageEnrichmentDispatcher(
            PlaceImageEnrichmentService enrichmentService,
            @Qualifier("placeImageExecutor") Executor executor
    ) {
        this.enrichmentService = enrichmentService;
        this.executor = executor;
    }

    public void dispatchImport(Long importId) {
        afterCommit(() -> submit(
                () -> enrichmentService.enrichImportPlaces(importId),
                () -> enrichmentService.recordImportDispatchFailure(importId)
        ));
    }

    public void dispatchPlace(Long placeId) {
        afterCommit(() -> submit(
                () -> enrichmentService.enrichPlace(placeId),
                () -> enrichmentService.recordPlaceDispatchFailure(placeId)
        ));
    }

    private void submit(Runnable task, Runnable rejectionHandler) {
        try {
            executor.execute(() -> runSafely(task));
        } catch (RejectedExecutionException exception) {
            rejectionHandler.run();
            logger.warn(
                    "Place image enrichment queued for retry: cause={}",
                    exception.getClass().getSimpleName()
            );
        }
    }

    private void runSafely(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException exception) {
            logger.warn(
                    "Place image enrichment task failed: cause={}",
                    exception.getClass().getSimpleName()
            );
        }
    }

    private void afterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }
}
