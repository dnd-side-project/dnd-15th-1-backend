package kr.omong.dulpick.domain.couple.application.support;

import kr.omong.dulpick.domain.couple.application.exception.ConnectionRateLimitExceededException;
import kr.omong.dulpick.domain.couple.config.ConnectionAbuseProperties;
import kr.omong.dulpick.domain.couple.domain.ConnectionAttempt;
import kr.omong.dulpick.domain.couple.domain.ConnectionAttemptRepository;
import kr.omong.dulpick.domain.couple.domain.ConnectionRateLimitSubject;
import kr.omong.dulpick.domain.couple.domain.ConnectionRateLimitSubjectRepository;
import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.global.exception.BusinessException;
import kr.omong.dulpick.global.exception.ErrorCode;
import kr.omong.dulpick.global.security.crypto.HmacSha256;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@EnableConfigurationProperties(ConnectionAbuseProperties.class)
public class ConnectionAbusePreventionService {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);
    private static final Duration TEN_MINUTES = Duration.ofMinutes(10);
    private static final Duration ONE_HOUR = Duration.ofHours(1);
    private static final Duration ONE_DAY = Duration.ofDays(1);
    private static final List<ConnectionAttempt.Action> STATE_CHANGE_ACTIONS = List.of(
            ConnectionAttempt.Action.CONNECT,
            ConnectionAttempt.Action.DISCONNECT
    );

    private final ConnectionAttemptRepository connectionAttemptRepository;
    private final ConnectionRateLimitSubjectRepository subjectRepository;
    private final ConnectionAbuseProperties properties;
    private final Clock clock;

    public ConnectionAbusePreventionService(
            ConnectionAttemptRepository connectionAttemptRepository,
            ConnectionRateLimitSubjectRepository subjectRepository,
            ConnectionAbuseProperties properties,
            Clock clock
    ) {
        this.connectionAttemptRepository = connectionAttemptRepository;
        this.subjectRepository = subjectRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public AttemptPermit begin(
            Long memberId,
            String clientAddress,
            ConnectionAttempt.Action action
    ) {
        Instant now = clock.instant();
        ConnectionRateLimitSubject subject = findOrCreateSubject(memberId, now);
        String ipHash = hashClientAddress(clientAddress);
        boolean allowed = !isRateLimited(memberId, ipHash, action, subject, now);
        ConnectionAttempt attempt = ConnectionAttempt.start(
                memberId,
                ipHash,
                action,
                now
        );
        if (!allowed) {
            attempt.complete(ConnectionAttempt.Outcome.RATE_LIMITED);
        }
        connectionAttemptRepository.save(attempt);
        return new AttemptPermit(attempt.getId(), allowed);
    }

    @Transactional
    public void completeSuccess(AttemptPermit permit) {
        complete(permit, ConnectionAttempt.Outcome.SUCCESS);
    }

    @Transactional
    public void completeFailure(
            Long memberId,
            AttemptPermit permit,
            BusinessException exception
    ) {
        boolean isCodeFailure = exception.getErrorCode() == ErrorCode.INVALID_CONNECTION_CODE;
        ConnectionAttempt.Outcome outcome = isCodeFailure
                ? ConnectionAttempt.Outcome.CODE_FAILURE
                : ConnectionAttempt.Outcome.BUSINESS_FAILURE;
        ConnectionRateLimitSubject subject = isCodeFailure
                ? lockSubject(memberId)
                : null;
        complete(permit, outcome);
        if (isCodeFailure) {
            blockAfterRepeatedFailures(memberId, subject);
        }
    }

    @Scheduled(
            initialDelayString = "${couple.abuse.cleanup-delay:1h}",
            fixedDelayString = "${couple.abuse.cleanup-delay:1h}"
    )
    @Transactional
    public void cleanupExpiredAttempts() {
        connectionAttemptRepository.deleteByCreatedAtBefore(
                clock.instant().minus(properties.retention())
        );
    }

    private ConnectionRateLimitSubject findOrCreateSubject(Long memberId, Instant now) {
        subjectRepository.createIfAbsent(memberId, now);
        return lockSubject(memberId);
    }

    private ConnectionRateLimitSubject lockSubject(Long memberId) {
        return subjectRepository.findForUpdateByMemberId(memberId)
                .orElseThrow(MemberNotFoundException::new);
    }

    private boolean isRateLimited(
            Long memberId,
            String ipHash,
            ConnectionAttempt.Action action,
            ConnectionRateLimitSubject subject,
            Instant now
    ) {
        if (isCodeValidationBlocked(action, subject, ipHash, now)) {
            return true;
        }
        return switch (action) {
            case PREVIEW -> exceedsPreviewLimit(memberId, now);
            case CONNECT -> exceedsConnectLimit(memberId, now)
                    || exceedsStateChangeLimit(memberId, now);
            case DISCONNECT -> exceedsStateChangeLimit(memberId, now);
        };
    }

    private boolean isCodeValidationBlocked(
            ConnectionAttempt.Action action,
            ConnectionRateLimitSubject subject,
            String ipHash,
            Instant now
    ) {
        if (action == ConnectionAttempt.Action.DISCONNECT) {
            return false;
        }
        if (subject.isBlocked(now)) {
            return true;
        }
        return ipHash != null && connectionAttemptRepository
                .countByIpHashAndOutcomeAndCreatedAtGreaterThanEqual(
                        ipHash,
                        ConnectionAttempt.Outcome.CODE_FAILURE,
                        now.minus(ONE_HOUR)
                ) >= properties.ipFailuresPerHour();
    }

    private boolean exceedsPreviewLimit(Long memberId, Instant now) {
        return count(memberId, ConnectionAttempt.Action.PREVIEW, now.minus(ONE_MINUTE))
                >= properties.previewPerMinute()
                || count(memberId, ConnectionAttempt.Action.PREVIEW, now.minus(ONE_HOUR))
                >= properties.previewPerHour();
    }

    private boolean exceedsConnectLimit(Long memberId, Instant now) {
        return count(memberId, ConnectionAttempt.Action.CONNECT, now.minus(ONE_MINUTE))
                >= properties.connectPerMinute()
                || count(memberId, ConnectionAttempt.Action.CONNECT, now.minus(ONE_DAY))
                >= properties.connectPerDay();
    }

    private boolean exceedsStateChangeLimit(Long memberId, Instant now) {
        return connectionAttemptRepository
                .countByMemberIdAndActionInAndCreatedAtGreaterThanEqual(
                        memberId,
                        STATE_CHANGE_ACTIONS,
                        now.minus(ONE_DAY)
                ) >= properties.stateChangesPerDay();
    }

    private long count(
            Long memberId,
            ConnectionAttempt.Action action,
            Instant since
    ) {
        return connectionAttemptRepository
                .countByMemberIdAndActionAndCreatedAtGreaterThanEqual(
                        memberId,
                        action,
                        since
                );
    }

    private void complete(AttemptPermit permit, ConnectionAttempt.Outcome outcome) {
        if (!permit.allowed()) {
            return;
        }
        ConnectionAttempt attempt = connectionAttemptRepository.findById(permit.attemptId())
                .orElseThrow(IllegalStateException::new);
        attempt.complete(outcome);
        connectionAttemptRepository.flush();
    }

    private void blockAfterRepeatedFailures(
            Long memberId,
            ConnectionRateLimitSubject subject
    ) {
        Instant now = clock.instant();
        long failureCount = connectionAttemptRepository
                .countByMemberIdAndOutcomeAndCreatedAtGreaterThanEqual(
                        memberId,
                        ConnectionAttempt.Outcome.CODE_FAILURE,
                        now.minus(TEN_MINUTES)
                );
        if (failureCount < properties.codeFailuresPerTenMinutes()) {
            return;
        }
        subject.blockUntil(now.plus(properties.failureBlockDuration()), now);
    }

    private String hashClientAddress(String clientAddress) {
        if (clientAddress == null || clientAddress.isBlank()) {
            return null;
        }
        return HmacSha256.hex(properties.ipHashKey(), clientAddress.strip());
    }

    public record AttemptPermit(Long attemptId, boolean allowed) {

        public void requireAllowed() {
            if (!allowed) {
                throw new ConnectionRateLimitExceededException();
            }
        }
    }
}
