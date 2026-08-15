package kr.omong.dulpick.domain.member.domain;

import kr.omong.dulpick.domain.member.domain.exception.InvalidMemberProfileException;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberProfileTest {

    private static final Instant NOW = Instant.parse("2026-08-07T00:00:00Z");
    private static final DatePreferences PREFERENCES = new DatePreferences(
            DatePreferenceOption.INDOOR,
            DatePreferenceOption.ACTIVE,
            DatePreferenceOption.NIGHT,
            DatePreferenceOption.FOOD
    );

    @Test
    void createsProfileWithNormalizedNicknameAndPreferences() {
        MemberProfile profile = MemberProfile.create(
                Member.create(Instant.EPOCH),
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
    void createsProfileWithoutDatePreferences() {
        MemberProfile profile = MemberProfile.create(
                Member.create(Instant.EPOCH),
                "둘픽이",
                1,
                null,
                NOW
        );

        assertThat(profile.getDatePreferences()).isNull();
    }

    @Test
    void countsExtendedEmojiAsOneUserPerceivedCharacter() {
        MemberProfile profile = MemberProfile.create(
                Member.create(Instant.EPOCH),
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
                DatePreferenceOption.OUTDOOR,
                DatePreferenceOption.STATIC,
                DatePreferenceOption.DAY,
                DatePreferenceOption.SIGHTSEEING
        );

        profile.updateBasicProfile(null, 3, NOW.plusSeconds(1));
        profile.updateDatePreferences(updatedPreferences, NOW.plusSeconds(2));

        assertThat(profile.getNickname()).isEqualTo("둘픽");
        assertThat(profile.getProfileIcon()).isEqualTo(3);
        assertThat(profile.getDatePreferences()).isEqualTo(updatedPreferences);
    }

    @Test
    void rejectsAnOptionFromAnotherPreferenceCategory() {
        assertThatThrownBy(() -> new DatePreferences(
                DatePreferenceOption.ACTIVE,
                DatePreferenceOption.INDOOR,
                DatePreferenceOption.FOOD,
                DatePreferenceOption.NIGHT
                )).isInstanceOf(InvalidMemberProfileException.class);
    }

    @Test
    void rejectsClearingDatePreferencesThroughUpdate() {
        MemberProfile profile = create("둘픽", 1);

        assertThatThrownBy(() -> profile.updateDatePreferences(null, NOW.plusSeconds(1)))
                .isInstanceOf(InvalidMemberProfileException.class);
    }

    @Test
    void rejectsProfileChangesForWithdrawnMember() {
        Member member = Member.create(Instant.EPOCH);
        MemberProfile profile = MemberProfile.create(member, "둘픽", 1, PREFERENCES, NOW);
        member.withdraw(NOW.plusSeconds(1));

        assertThatThrownBy(() -> profile.updateBasicProfile(
                "새닉네임",
                null,
                NOW.plusSeconds(2)
        )).isInstanceOf(MemberNotActiveException.class);
        assertThatThrownBy(() -> profile.updateDatePreferences(
                PREFERENCES,
                NOW.plusSeconds(2)
        )).isInstanceOf(MemberNotActiveException.class);
    }

    private MemberProfile create(String nickname, int profileIcon) {
        return MemberProfile.create(
                Member.create(Instant.EPOCH),
                nickname,
                profileIcon,
                PREFERENCES,
                NOW
        );
    }
}
