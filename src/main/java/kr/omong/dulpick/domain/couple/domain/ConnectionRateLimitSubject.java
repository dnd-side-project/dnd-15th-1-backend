package kr.omong.dulpick.domain.couple.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "connection_rate_limit_subjects")
public class ConnectionRateLimitSubject {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "blocked_until")
    private Instant blockedUntil;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ConnectionRateLimitSubject() {
    }

    public boolean isBlocked(Instant now) {
        return blockedUntil != null && blockedUntil.isAfter(now);
    }

    public void blockUntil(Instant blockedUntil, Instant updatedAt) {
        if (this.blockedUntil == null || this.blockedUntil.isBefore(blockedUntil)) {
            this.blockedUntil = blockedUntil;
        }
        this.updatedAt = updatedAt;
    }
}
