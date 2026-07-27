package kr.omong.dulpick.domain.member.domain;

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

    protected Member() {
    }

    private Member(MemberStatus status, Instant createdAt) {
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static Member create() {
        return new Member(MemberStatus.ACTIVE, Instant.now());
    }

    public Long getId() {
        return id;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == MemberStatus.ACTIVE;
    }

    public void withdraw() {
        status = MemberStatus.WITHDRAWN;
        updatedAt = Instant.now();
    }
}
