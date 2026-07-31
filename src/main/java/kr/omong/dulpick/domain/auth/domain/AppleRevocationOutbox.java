package kr.omong.dulpick.domain.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "apple_revocation_outbox")
public class AppleRevocationOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "encrypted_refresh_token", nullable = false, length = 2048)
    private String encryptedRefreshToken;

    @Column(name = "client_id", nullable = false, length = 255)
    private String clientId;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AppleRevocationOutbox() {
    }

    private AppleRevocationOutbox(
            Long memberId,
            String encryptedRefreshToken,
            String clientId,
            Instant createdAt
    ) {
        this.memberId = memberId;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.clientId = clientId;
        this.nextAttemptAt = createdAt;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static AppleRevocationOutbox create(
            Long memberId,
            String encryptedRefreshToken,
            String clientId,
            Instant createdAt
    ) {
        return new AppleRevocationOutbox(
                memberId,
                encryptedRefreshToken,
                clientId,
                createdAt
        );
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getEncryptedRefreshToken() {
        return encryptedRefreshToken;
    }

    public String getClientId() {
        return clientId;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void scheduleRetry(
            Instant failedAt,
            Duration initialDelay,
            Duration maxDelay
    ) {
        attemptCount++;
        Duration retryDelay = calculateRetryDelay(initialDelay, maxDelay);
        nextAttemptAt = failedAt.plus(retryDelay);
        updatedAt = failedAt;
    }

    private Duration calculateRetryDelay(Duration initialDelay, Duration maxDelay) {
        int exponent = Math.min(attemptCount - 1, 20);
        Duration retryDelay = initialDelay.multipliedBy(1L << exponent);
        if (retryDelay.compareTo(maxDelay) > 0) {
            return maxDelay;
        }
        return retryDelay;
    }
}
