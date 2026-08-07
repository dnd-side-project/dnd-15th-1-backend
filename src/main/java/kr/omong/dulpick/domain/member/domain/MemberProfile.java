package kr.omong.dulpick.domain.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import kr.omong.dulpick.domain.member.domain.exception.InvalidMemberProfileException;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@Table(name = "member_profiles")
public class MemberProfile {

    private static final int MIN_NICKNAME_LENGTH = 1;
    private static final int MAX_NICKNAME_LENGTH = 6;
    private static final int MIN_PROFILE_ICON = 1;
    private static final int MAX_PROFILE_ICON = 5;
    private static final Pattern GRAPHEME_PATTERN = Pattern.compile("\\X");
    private static final Pattern CONTROL_PATTERN = Pattern.compile("\\p{Cc}");

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 64)
    private String nickname;

    @Column(name = "profile_icon", nullable = false)
    private byte profileIcon;

    @Enumerated(EnumType.STRING)
    @Column(name = "indoor_outdoor", nullable = false, length = 20)
    private DatePreferenceOption indoorOutdoor;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", nullable = false, length = 20)
    private DatePreferenceOption activityLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "date_time", nullable = false, length = 20)
    private DatePreferenceOption dateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "date_focus", nullable = false, length = 20)
    private DatePreferenceOption dateFocus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MemberProfile() {
    }

    private MemberProfile(
            Member member,
            String nickname,
            int profileIcon,
            DatePreferences preferences,
            Instant createdAt
    ) {
        this.member = member;
        this.nickname = normalizeNickname(nickname);
        this.profileIcon = validateProfileIcon(profileIcon);
        updatePreferences(preferences);
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static MemberProfile create(
            Member member,
            String nickname,
            int profileIcon,
            DatePreferences preferences,
            Instant createdAt
    ) {
        if (member == null || createdAt == null) {
            throw new InvalidMemberProfileException();
        }
        return new MemberProfile(member, nickname, profileIcon, preferences, createdAt);
    }

    public void updateBasicProfile(String nickname, Integer profileIcon, Instant updatedAt) {
        if (nickname == null && profileIcon == null) {
            throw new InvalidMemberProfileException();
        }
        if (nickname != null) {
            this.nickname = normalizeNickname(nickname);
        }
        if (profileIcon != null) {
            this.profileIcon = validateProfileIcon(profileIcon);
        }
        this.updatedAt = requireUpdatedAt(updatedAt);
    }

    public void updateDatePreferences(DatePreferences preferences, Instant updatedAt) {
        updatePreferences(preferences);
        this.updatedAt = requireUpdatedAt(updatedAt);
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getNickname() {
        return nickname;
    }

    public int getProfileIcon() {
        return profileIcon;
    }

    public DatePreferences getDatePreferences() {
        return new DatePreferences(indoorOutdoor, activityLevel, dateTime, dateFocus);
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void updatePreferences(DatePreferences preferences) {
        if (preferences == null) {
            throw new InvalidMemberProfileException();
        }
        indoorOutdoor = preferences.indoorOutdoor();
        activityLevel = preferences.activityLevel();
        dateTime = preferences.dateTime();
        dateFocus = preferences.dateFocus();
    }

    private static String normalizeNickname(String nickname) {
        if (nickname == null) {
            throw new InvalidMemberProfileException();
        }
        String normalized = nickname.strip();
        if (normalized.isBlank() || CONTROL_PATTERN.matcher(normalized).find()) {
            throw new InvalidMemberProfileException();
        }
        Matcher matcher = GRAPHEME_PATTERN.matcher(normalized);
        int length = 0;
        while (matcher.find()) {
            length++;
        }
        if (length < MIN_NICKNAME_LENGTH || length > MAX_NICKNAME_LENGTH) {
            throw new InvalidMemberProfileException();
        }
        return normalized;
    }

    private static byte validateProfileIcon(int profileIcon) {
        if (profileIcon < MIN_PROFILE_ICON || profileIcon > MAX_PROFILE_ICON) {
            throw new InvalidMemberProfileException();
        }
        return (byte) profileIcon;
    }

    private Instant requireUpdatedAt(Instant updatedAt) {
        if (updatedAt == null) {
            throw new InvalidMemberProfileException();
        }
        return updatedAt;
    }
}
