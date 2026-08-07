package kr.omong.dulpick.domain.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;

import java.time.Instant;

@Entity
@Table(name = "member_notification_settings")
public class MemberNotificationSettings {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "content_saved_enabled", nullable = false)
    private boolean contentSavedEnabled;

    @Column(name = "date_schedule_enabled", nullable = false)
    private boolean dateScheduleEnabled;

    @Column(name = "marketing_enabled", nullable = false)
    private boolean marketingEnabled;

    @Column(name = "marketing_consent_version", length = 30)
    private String marketingConsentVersion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MemberNotificationSettings() {
    }

    private MemberNotificationSettings(Member member, Instant createdAt) {
        this.member = member;
        this.contentSavedEnabled = true;
        this.dateScheduleEnabled = true;
        this.marketingEnabled = false;
        this.marketingConsentVersion = null;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static MemberNotificationSettings create(Member member, Instant createdAt) {
        if (member == null || createdAt == null) {
            throw new IllegalArgumentException("Member notification settings are required");
        }
        if (!member.isActive()) {
            throw new MemberNotActiveException();
        }
        return new MemberNotificationSettings(member, createdAt);
    }

    public boolean update(
            boolean contentSavedEnabled,
            boolean dateScheduleEnabled,
            boolean marketingEnabled,
            String marketingConsentVersion,
            Instant updatedAt
    ) {
        validateMemberActive();
        boolean marketingChanged = this.marketingEnabled != marketingEnabled;
        this.contentSavedEnabled = contentSavedEnabled;
        this.dateScheduleEnabled = dateScheduleEnabled;
        this.marketingEnabled = marketingEnabled;
        this.marketingConsentVersion = marketingEnabled ? marketingConsentVersion : null;
        this.updatedAt = updatedAt;
        return marketingChanged;
    }

    public boolean isContentSavedEnabled() {
        return contentSavedEnabled;
    }

    public boolean isDateScheduleEnabled() {
        return dateScheduleEnabled;
    }

    public boolean isMarketingEnabled() {
        return marketingEnabled;
    }

    public String getMarketingConsentVersion() {
        return marketingConsentVersion;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void validateMemberActive() {
        if (!member.isActive()) {
            throw new MemberNotActiveException();
        }
    }
}
