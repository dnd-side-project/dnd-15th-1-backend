package kr.omong.dulpick.domain.couple.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import kr.omong.dulpick.domain.member.domain.Member;

import java.time.Instant;

@Entity
@Table(name = "couples")
public class Couple {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CoupleStatus status;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt;

    @Column(name = "disconnected_at")
    private Instant disconnectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disconnected_by_member_id")
    private Member disconnectedByMember;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Couple() {
    }

    private Couple(Instant connectedAt) {
        this.status = CoupleStatus.ACTIVE;
        this.connectedAt = connectedAt;
        this.createdAt = connectedAt;
        this.updatedAt = connectedAt;
    }

    public static Couple connect(Instant connectedAt) {
        return new Couple(connectedAt);
    }

    public void disconnect(Member requestedBy, Instant disconnectedAt) {
        if (status != CoupleStatus.ACTIVE) {
            return;
        }
        status = CoupleStatus.DISCONNECTED;
        disconnectedByMember = requestedBy;
        this.disconnectedAt = disconnectedAt;
        updatedAt = disconnectedAt;
    }

    public Long getId() {
        return id;
    }

    public CoupleStatus getStatus() {
        return status;
    }

    public Instant getConnectedAt() {
        return connectedAt;
    }
}
