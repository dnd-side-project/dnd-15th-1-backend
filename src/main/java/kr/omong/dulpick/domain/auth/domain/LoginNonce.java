package kr.omong.dulpick.domain.auth.domain;

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
@Table(name = "login_nonces")
public class LoginNonce {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "nonce_hash", nullable = false, length = 64)
    private String nonceHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LoginNonce() {
    }

    private LoginNonce(SocialProvider provider, String nonceHash, Instant expiresAt) {
        this.provider = provider;
        this.nonceHash = nonceHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public static LoginNonce create(SocialProvider provider, String nonceHash, Instant expiresAt) {
        return new LoginNonce(provider, nonceHash, expiresAt);
    }

    public boolean isUsable(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    public void use() {
        usedAt = Instant.now();
    }
}
