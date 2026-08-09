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
import kr.omong.dulpick.domain.couple.domain.exception.ConnectionCodeNotActiveException;
import kr.omong.dulpick.domain.member.domain.Member;

import java.time.Instant;

@Entity
@Table(name = "connection_codes")
public class ConnectionCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "code_digest", nullable = false, unique = true, length = 64)
    private String codeDigest;

    @Column(name = "encrypted_code", nullable = false, length = 255)
    private String encryptedCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConnectionCodeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "issued_reason", nullable = false, length = 30)
    private ConnectionCodeIssuedReason issuedReason;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ConnectionCode() {
    }

    private ConnectionCode(
            Member member,
            String codeDigest,
            String encryptedCode,
            ConnectionCodeIssuedReason issuedReason,
            Instant createdAt
    ) {
        this.member = member;
        this.codeDigest = codeDigest;
        this.encryptedCode = encryptedCode;
        this.status = ConnectionCodeStatus.ACTIVE;
        this.issuedReason = issuedReason;
        this.createdAt = createdAt;
    }

    public static ConnectionCode issue(
            Member member,
            String codeDigest,
            String encryptedCode,
            ConnectionCodeIssuedReason issuedReason,
            Instant createdAt
    ) {
        return new ConnectionCode(
                member,
                codeDigest,
                encryptedCode,
                issuedReason,
                createdAt
        );
    }

    public void use(Instant usedAt) {
        if (status != ConnectionCodeStatus.ACTIVE) {
            throw new ConnectionCodeNotActiveException();
        }
        status = ConnectionCodeStatus.USED;
        this.usedAt = usedAt;
    }

    public void revoke(Instant revokedAt) {
        if (status != ConnectionCodeStatus.ACTIVE) {
            throw new ConnectionCodeNotActiveException();
        }
        status = ConnectionCodeStatus.REVOKED;
        this.revokedAt = revokedAt;
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public String getCodeDigest() {
        return codeDigest;
    }

    public String getEncryptedCode() {
        return encryptedCode;
    }

    public ConnectionCodeStatus getStatus() {
        return status;
    }
}
