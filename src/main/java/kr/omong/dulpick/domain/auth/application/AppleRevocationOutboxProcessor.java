package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.auth.domain.AppleRevocationOutboxRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
@EnableConfigurationProperties(AppleRevocationOutboxProperties.class)
public class AppleRevocationOutboxProcessor {

    private final AppleRevocationOutboxRepository outboxRepository;
    private final AppleRevocationOutboxWorker outboxWorker;
    private final AppleRevocationOutboxProperties properties;
    private final Clock clock;

    public AppleRevocationOutboxProcessor(
            AppleRevocationOutboxRepository outboxRepository,
            AppleRevocationOutboxWorker outboxWorker,
            AppleRevocationOutboxProperties properties,
            Clock clock
    ) {
        this.outboxRepository = outboxRepository;
        this.outboxWorker = outboxWorker;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            initialDelayString = "${auth.apple.revocation.process-delay:1m}",
            fixedDelayString = "${auth.apple.revocation.process-delay:1m}"
    )
    public void process() {
        outboxRepository.findRetryableIds(
                clock.instant(),
                PageRequest.of(0, properties.batchSize())
        ).forEach(outboxWorker::process);
    }
}
