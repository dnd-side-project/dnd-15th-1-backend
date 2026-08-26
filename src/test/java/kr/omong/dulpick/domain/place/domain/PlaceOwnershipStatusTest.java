package kr.omong.dulpick.domain.place.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceOwnershipStatusTest {

    @Test
    void resolvesTogetherOnlyWhenBothMembersSaved() {
        assertThat(PlaceOwnershipStatus.resolve(true, true))
                .isEqualTo(PlaceOwnershipStatus.TOGETHER);
        assertThat(PlaceOwnershipStatus.resolve(true, false))
                .isEqualTo(PlaceOwnershipStatus.MINE);
        assertThat(PlaceOwnershipStatus.resolve(false, true))
                .isEqualTo(PlaceOwnershipStatus.PARTNER);
    }

    @Test
    void resolvesMineOrPartnerWithoutActiveCouple() {
        assertThat(PlaceOwnershipStatus.resolve(true, false))
                .isEqualTo(PlaceOwnershipStatus.MINE);
        assertThat(PlaceOwnershipStatus.resolve(false, true))
                .isEqualTo(PlaceOwnershipStatus.PARTNER);
    }

    @Test
    void matchesTogetherFilterOnlyWithBothSaves() {
        assertThat(PlaceOwnershipStatus.matchesFilter(
                PlaceOwnershipStatus.TOGETHER,
                true,
                false
        )).isFalse();
        assertThat(PlaceOwnershipStatus.matchesFilter(
                PlaceOwnershipStatus.TOGETHER,
                false,
                true
        )).isFalse();
        assertThat(PlaceOwnershipStatus.matchesFilter(
                PlaceOwnershipStatus.TOGETHER,
                true,
                true
        )).isTrue();
        assertThat(PlaceOwnershipStatus.matchesFilter(
                PlaceOwnershipStatus.TOGETHER,
                false,
                false
        )).isFalse();
    }
}
