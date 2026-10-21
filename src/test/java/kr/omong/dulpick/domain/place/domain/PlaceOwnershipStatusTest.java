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
    void matchesTogetherFilterWithEitherSave() {
        assertThat(PlaceOwnershipStatus.matchesFilter(PlaceOwnershipStatus.TOGETHER, true, false)).isTrue();
        assertThat(PlaceOwnershipStatus.matchesFilter(PlaceOwnershipStatus.TOGETHER, false, true)).isTrue();
        assertThat(PlaceOwnershipStatus.matchesFilter(PlaceOwnershipStatus.TOGETHER, true, true)).isTrue();
        assertThat(PlaceOwnershipStatus.matchesFilter(PlaceOwnershipStatus.TOGETHER, false, false)).isFalse();
    }

    @Test
    void keepsMineAndPartnerFiltersExclusive() {
        assertThat(PlaceOwnershipStatus.matchesFilter(PlaceOwnershipStatus.MINE, true, false)).isTrue();
        assertThat(PlaceOwnershipStatus.matchesFilter(PlaceOwnershipStatus.MINE, true, true)).isFalse();
        assertThat(PlaceOwnershipStatus.matchesFilter(PlaceOwnershipStatus.MINE, false, true)).isFalse();
        assertThat(PlaceOwnershipStatus.matchesFilter(PlaceOwnershipStatus.PARTNER, false, true)).isTrue();
        assertThat(PlaceOwnershipStatus.matchesFilter(PlaceOwnershipStatus.PARTNER, true, true)).isFalse();
        assertThat(PlaceOwnershipStatus.matchesFilter(PlaceOwnershipStatus.PARTNER, true, false)).isFalse();
    }

    @Test
    void withoutFilterEveryVisiblePlaceMatches() {
        assertThat(PlaceOwnershipStatus.matchesFilter(null, true, false)).isTrue();
        assertThat(PlaceOwnershipStatus.matchesFilter(null, false, true)).isTrue();
        assertThat(PlaceOwnershipStatus.matchesFilter(null, true, true)).isTrue();
    }
}
