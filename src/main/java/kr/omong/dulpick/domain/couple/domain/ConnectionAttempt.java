package kr.omong.dulpick.domain.couple.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "connection_attempts")
public class ConnectionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "ip_hash", nullable = false, length = 64)
    private String ipHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Action action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Outcome outcome;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ConnectionAttempt() {
    }

    private ConnectionAttempt(
            Long memberId,
            String ipHash,
            Action action,
            Outcome outcome,
            Instant createdAt
    ) {
        this.memberId = memberId;
        this.ipHash = ipHash;
        this.action = action;
        this.outcome = outcome;
        this.createdAt = createdAt;
    }

    public static ConnectionAttempt start(
            Long memberId,
            String ipHash,
            Action action,
            Instant createdAt
    ) {
        return new ConnectionAttempt(
                memberId,
                ipHash,
                action,
                Outcome.ATTEMPTED,
                createdAt
        );
    }

    public void complete(Outcome outcome) {
        this.outcome = outcome;
    }

    public Long getId() {
        return id;
    }

    public enum Action {
        PREVIEW,
        CONNECT,
        DISCONNECT
    }

    public enum Outcome {
        ATTEMPTED,
        SUCCESS,
        BUSINESS_FAILURE,
        CODE_FAILURE,
        RATE_LIMITED
    }
}
