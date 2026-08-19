package kr.omong.dulpick.domain.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import kr.omong.dulpick.domain.member.domain.Member;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_token_hash", length = 64)
    private String replacedByTokenHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RefreshToken() {
    }

    private RefreshToken(
            Member member,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt
    ) {
        this.member = member;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static RefreshToken create(
            Member member,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt
    ) {
        return new RefreshToken(member, tokenHash, expiresAt, createdAt);
    }

    public Member getMember() {
        return member;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean wasRotated() {
        return replacedByTokenHash != null;
    }

    public boolean isWithinReplayGrace(Instant now, Duration replayGrace) {
        return revokedAt != null && revokedAt.plus(replayGrace).isAfter(now);
    }

    public boolean matchesReplacementHash(String candidateHash) {
        return replacedByTokenHash != null && replacedByTokenHash.equals(candidateHash);
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void rotate(String replacementTokenHash, Instant rotatedAt) {
        this.revokedAt = rotatedAt;
        this.replacedByTokenHash = replacementTokenHash;
    }

    public void revoke(Instant revokedAt) {
        if (this.revokedAt == null) {
            this.revokedAt = revokedAt;
        }
    }
}
