package kr.omong.dulpick.domain.place.application.scheduled;

import kr.omong.dulpick.domain.place.application.PlaceImportDispatcher;
import kr.omong.dulpick.domain.place.config.PlaceAnalysisProperties;
import kr.omong.dulpick.domain.place.domain.PlaceImportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
@ConditionalOnProperty(
        name = "place-analysis.recovery-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PlaceImportRecoveryProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PlaceImportRecoveryProcessor.class);

    private final PlaceImportRepository importRepository;
    private final PlaceImportDispatcher dispatcher;
    private final PlaceAnalysisProperties properties;
    private final Clock clock;

    public PlaceImportRecoveryProcessor(
            PlaceImportRepository importRepository,
            PlaceImportDispatcher dispatcher,
            PlaceAnalysisProperties properties,
            Clock clock
    ) {
        this.importRepository = importRepository;
        this.dispatcher = dispatcher;
        this.properties = properties;
        this.clock = clock;
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
            dispatcher.dispatch(importId);
        } catch (RuntimeException exception) {
            logger.error("Place import dispatch failed: importId={}", importId, exception);
        }
    }
}
