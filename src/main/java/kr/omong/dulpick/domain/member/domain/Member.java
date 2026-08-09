package kr.omong.dulpick.domain.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.omong.dulpick.domain.member.domain.exception.MemberAlreadyWithdrawnException;

import java.time.Instant;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_withdrawn_at")
    private Instant lastWithdrawnAt;

    @Column(name = "last_rejoined_at")
    private Instant lastRejoinedAt;

    @Column(name = "token_version", nullable = false)
    private long tokenVersion;

    protected Member() {
    }

    private Member(MemberStatus status, Instant createdAt) {
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static Member create(Instant createdAt) {
        return new Member(MemberStatus.ACTIVE, createdAt);
    }

    public Long getId() {
        return id;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getLastWithdrawnAt() {
        return lastWithdrawnAt;
    }

    public Instant getLastRejoinedAt() {
        return lastRejoinedAt;
    }

    public long getTokenVersion() {
        return tokenVersion;
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    public void withdraw(Instant withdrawnAt) {
        if (!isActive()) {
            throw new MemberAlreadyWithdrawnException();
        }
        status = MemberStatus.WITHDRAWN;
        lastWithdrawnAt = withdrawnAt;
        tokenVersion++;
        updatedAt = withdrawnAt;
    }

    public void rejoin(Instant rejoinedAt) {
        status = MemberStatus.ACTIVE;
        lastRejoinedAt = rejoinedAt;
        updatedAt = rejoinedAt;
    }
}
