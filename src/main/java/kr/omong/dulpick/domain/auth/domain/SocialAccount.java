package kr.omong.dulpick.domain.auth.domain;

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
@Table(name = "social_accounts")
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "provider_subject", nullable = false, length = 255)
    private String providerSubject;

    @Column(length = 320)
    private String email;

    @Column(name = "provider_refresh_token", length = 2048)
    private String providerRefreshToken;

    @Column(name = "provider_client_id", length = 255)
    private String providerClientId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SocialAccount() {
    }

    private SocialAccount(
            Member member,
            SocialProvider provider,
            String providerSubject,
            String email,
            Instant createdAt
    ) {
        this.member = member;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.email = email;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static SocialAccount create(
            Member member,
            SocialProvider provider,
            String providerSubject,
            String email,
            Instant createdAt
    ) {
        return new SocialAccount(member, provider, providerSubject, email, createdAt);
    }

    public Member getMember() {
        return member;
    }

    public SocialProvider getProvider() {
        return provider;
    }

    public String getProviderSubject() {
        return providerSubject;
    }

    public String getEmail() {
        return email;
    }

    public void updateEmail(String email, Instant updatedAt) {
        this.email = email;
        this.updatedAt = updatedAt;
    }

    public String getProviderRefreshToken() {
        return providerRefreshToken;
    }

    public String getProviderClientId() {
        return providerClientId;
    }

    public void updateProviderAuthorization(
            String encryptedRefreshToken,
            String clientId,
            Instant updatedAt
    ) {
        this.providerRefreshToken = encryptedRefreshToken;
        this.providerClientId = clientId;
        this.updatedAt = updatedAt;
    }

    public void updateProviderClientIdWhenTokenIsAbsent(
            String clientId,
            Instant updatedAt
    ) {
        if (providerRefreshToken != null) {
            return;
        }
        this.providerClientId = clientId;
        this.updatedAt = updatedAt;
    }

    public void clearProviderAuthorization(Instant updatedAt) {
        this.providerRefreshToken = null;
        this.providerClientId = null;
        this.updatedAt = updatedAt;
    }

    public void reassignMember(Member member, Instant updatedAt) {
        this.member = member;
        this.updatedAt = updatedAt;
    }
}
