package kr.omong.dulpick.domain.place.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceOwnershipStatusTest {

    @Test
    void resolvesTogetherWhenCoupleHasAnySave() {
        assertThat(PlaceOwnershipStatus.resolve(true, true, false))
                .isEqualTo(PlaceOwnershipStatus.TOGETHER);
        assertThat(PlaceOwnershipStatus.resolve(true, false, true))
                .isEqualTo(PlaceOwnershipStatus.TOGETHER);
        assertThat(PlaceOwnershipStatus.resolve(true, true, true))
                .isEqualTo(PlaceOwnershipStatus.TOGETHER);
    }

    @Test
    void resolvesMineOrPartnerWithoutActiveCouple() {
        assertThat(PlaceOwnershipStatus.resolve(false, true, false))
                .isEqualTo(PlaceOwnershipStatus.MINE);
        assertThat(PlaceOwnershipStatus.resolve(false, false, true))
                .isEqualTo(PlaceOwnershipStatus.PARTNER);
    }

    @Test
    void matchesTogetherFilterWithEitherSave() {
        assertThat(PlaceOwnershipStatus.matchesFilter(
                PlaceOwnershipStatus.TOGETHER,
                true,
                false
        )).isTrue();
        assertThat(PlaceOwnershipStatus.matchesFilter(
                PlaceOwnershipStatus.TOGETHER,
                false,
                true
        )).isTrue();
        assertThat(PlaceOwnershipStatus.matchesFilter(
                PlaceOwnershipStatus.TOGETHER,
                false,
                false
        )).isFalse();
    }
}
