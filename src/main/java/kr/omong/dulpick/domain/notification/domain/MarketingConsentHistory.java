package kr.omong.dulpick.domain.notification.domain;

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

import java.time.Instant;

@Entity
@Table(name = "marketing_consent_histories")
public class MarketingConsentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private boolean consented;

    @Column(name = "consent_version", nullable = false, length = 30)
    private String consentVersion;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected MarketingConsentHistory() {
    }

    private MarketingConsentHistory(
            Member member,
            boolean consented,
            String consentVersion,
            Instant changedAt
    ) {
        this.member = member;
        this.consented = consented;
        this.consentVersion = consentVersion;
        this.changedAt = changedAt;
    }

    public static MarketingConsentHistory record(
            Member member,
            boolean consented,
            String consentVersion,
            Instant changedAt
    ) {
        return new MarketingConsentHistory(
                member,
                consented,
                consentVersion,
                changedAt
        );
    }
}
