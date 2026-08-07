package kr.omong.dulpick.domain.member.domain;

import kr.omong.dulpick.domain.member.domain.exception.InvalidMemberProfileException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberProfileTest {

    private static final Instant NOW = Instant.parse("2026-08-07T00:00:00Z");
    private static final DatePreferences PREFERENCES = new DatePreferences(
            IndoorOutdoor.INDOOR,
            ActivityLevel.ACTIVE,
            DateTimePreference.NIGHT,
            DateFocus.FOOD
    );

    @Test
    void createsProfileWithNormalizedNicknameAndPreferences() {
        MemberProfile profile = MemberProfile.create(
                Member.create(),
                "  둘픽이  ",
                1,
                PREFERENCES,
                NOW
        );

        assertThat(profile.getNickname()).isEqualTo("둘픽이");
        assertThat(profile.getProfileIcon()).isEqualTo(1);
        assertThat(profile.getDatePreferences()).isEqualTo(PREFERENCES);
    }

    @Test
    void countsExtendedEmojiAsOneUserPerceivedCharacter() {
        MemberProfile profile = MemberProfile.create(
                Member.create(),
                "👩‍❤️‍👨둘픽",
                5,
                PREFERENCES,
                NOW
        );

        assertThat(profile.getNickname()).isEqualTo("👩‍❤️‍👨둘픽");
    }

    @Test
    void rejectsNicknameOutsideOneToSixCharacters() {
        assertThatThrownBy(() -> create("", 1))
                .isInstanceOf(InvalidMemberProfileException.class);
        assertThatThrownBy(() -> create("일이삼사오육칠", 1))
                .isInstanceOf(InvalidMemberProfileException.class);
    }

    @Test
    void rejectsProfileIconOutsideOneToFive() {
        assertThatThrownBy(() -> create("둘픽", 0))
                .isInstanceOf(InvalidMemberProfileException.class);
        assertThatThrownBy(() -> create("둘픽", 6))
                .isInstanceOf(InvalidMemberProfileException.class);
    }

    @Test
    void updatesBasicProfileAndAllDatePreferences() {
        MemberProfile profile = create("둘픽", 1);
        DatePreferences updatedPreferences = new DatePreferences(
                IndoorOutdoor.OUTDOOR,
                ActivityLevel.STATIC,
                DateTimePreference.DAY,
                DateFocus.SIGHTSEEING
        );

        profile.updateBasicProfile(null, 3, NOW.plusSeconds(1));
        profile.updateDatePreferences(updatedPreferences, NOW.plusSeconds(2));

        assertThat(profile.getNickname()).isEqualTo("둘픽");
        assertThat(profile.getProfileIcon()).isEqualTo(3);
        assertThat(profile.getDatePreferences()).isEqualTo(updatedPreferences);
    }

    private MemberProfile create(String nickname, int profileIcon) {
        return MemberProfile.create(
                Member.create(),
                nickname,
                profileIcon,
                PREFERENCES,
                NOW
        );
    }
}
