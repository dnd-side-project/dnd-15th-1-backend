package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.auth.domain.LoginNonceRepository;
import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@Service
@EnableConfigurationProperties(AuthenticationDataCleanupProperties.class)
public class AuthenticationDataCleanupService {

    private final LoginNonceRepository loginNonceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationDataCleanupProperties properties;
    private final Clock clock;

    public AuthenticationDataCleanupService(
            LoginNonceRepository loginNonceRepository,
            RefreshTokenRepository refreshTokenRepository,
            AuthenticationDataCleanupProperties properties,
            Clock clock
    ) {
        this.loginNonceRepository = loginNonceRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            initialDelayString = "${auth.maintenance.fixed-delay:1h}",
            fixedDelayString = "${auth.maintenance.fixed-delay:1h}"
    )
    @Transactional
    public void cleanup() {
        Instant now = clock.instant();
        int deletedNonces = deleteNonceBatches(now);
        int deletedRefreshTokens = deleteRefreshTokenBatches(now);
        if (deletedNonces > 0 || deletedRefreshTokens > 0) {
            log.info(
                    "Authentication data cleanup completed: nonces={}, refreshTokens={}",
                    deletedNonces,
                    deletedRefreshTokens
            );
        }
    }

    private int deleteNonceBatches(Instant now) {
        return deleteInBatches(() ->
                loginNonceRepository.deleteUsedBefore(now, properties.batchSize())
                        + loginNonceRepository.deleteExpiredBefore(now, properties.batchSize())
        );
    }

    private int deleteRefreshTokenBatches(Instant now) {
        Instant revokedBefore = now.minus(properties.revokedRefreshTokenRetention());
        return deleteInBatches(() ->
                refreshTokenRepository.deleteExpiredBefore(now, properties.batchSize())
                        + refreshTokenRepository.deleteRevokedBeforeWithoutRotation(
                        revokedBefore,
                        properties.batchSize()
                )
        );
    }

    private int deleteInBatches(DeleteBatch deleteBatch) {
        int total = 0;
        for (int batch = 0; batch < properties.maxBatchesPerRun(); batch++) {
            int deleted = deleteBatch.execute();
            total += deleted;
            if (deleted < properties.batchSize()) {
                break;
            }
        }
        return total;
    }

    @FunctionalInterface
    private interface DeleteBatch {

        int execute();
    }
}
