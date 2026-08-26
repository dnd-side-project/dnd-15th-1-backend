package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMember;
import kr.omong.dulpick.domain.couple.domain.ActiveCoupleMemberRepository;
import kr.omong.dulpick.domain.couple.domain.Couple;
import kr.omong.dulpick.domain.place.domain.MemberPlace;
import kr.omong.dulpick.domain.place.domain.MemberPlaceRepository;
import kr.omong.dulpick.domain.place.domain.DulpickPlaceCategory;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceOwnershipStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaceQueryServiceTest {

    private final MemberPlaceRepository memberPlaceRepository = mock(MemberPlaceRepository.class);
    private final ActiveCoupleMemberRepository coupleMemberRepository =
            mock(ActiveCoupleMemberRepository.class);
    private final PlaceQueryService service = new PlaceQueryService(
            memberPlaceRepository,
            coupleMemberRepository
    );

    @Test
    void collapsesSamePlaceSavedByBothMembersIntoTogether() {
        Place place = place(10L);
        MemberPlace mine = memberPlace(1L, place, "내 별칭", Instant.parse("2026-08-10T00:00:00Z"));
        MemberPlace partner = memberPlace(
                2L,
                place,
                "상대 별칭",
                Instant.parse("2026-08-10T01:00:00Z")
        );
        connectCouple(1L, 2L);
        when(memberPlaceRepository.findAllByMemberIdInOrderBySavedAtDesc(List.of(1L, 2L)))
                .thenReturn(List.of(partner, mine));

        List<MemberPlaceView> result = service.getVisiblePlaces(1L);

        assertThat(result).singleElement().satisfies(saved -> {
            assertThat(saved.placeId()).isEqualTo(10L);
            assertThat(saved.memberId()).isEqualTo(1L);
            assertThat(saved.ownershipStatus()).isEqualTo(PlaceOwnershipStatus.TOGETHER);
            assertThat(saved.alias()).isEqualTo("내 별칭");
        });
    }

    @Test
    void includesSavedMemberCount() {
        Place place = place(10L);
        MemberPlace mine = memberPlace(1L, place, "내 별칭", Instant.parse("2026-08-10T00:00:00Z"));
        when(coupleMemberRepository.findByMemberId(1L)).thenReturn(Optional.empty());
        when(memberPlaceRepository.findAllByMemberIdInOrderBySavedAtDesc(List.of(1L)))
                .thenReturn(List.of(mine));
        when(memberPlaceRepository.countSavesByPlaceIdIn(List.of(10L)))
                .thenReturn(saveCounts(10L, 2L));

        List<MemberPlaceView> result = service.getVisiblePlaces(1L);

        assertThat(result).singleElement().satisfies(saved ->
                assertThat(saved.savedMemberCount()).isEqualTo(2)
        );
    }

    @Test
    void marksPartnerOnlyPlaceAsPartnerWhenCoupleIsActive() {
        Place place = place(20L);
        MemberPlace partner = memberPlace(
                2L,
                place,
                "상대 별칭",
                Instant.parse("2026-08-10T01:00:00Z")
        );
        connectCouple(1L, 2L);
        when(memberPlaceRepository.findAllByMemberIdInOrderBySavedAtDesc(List.of(1L, 2L)))
                .thenReturn(List.of(partner));

        assertThat(service.getVisiblePlaces(1L)).singleElement().satisfies(saved ->
                assertThat(saved.ownershipStatus()).isEqualTo(PlaceOwnershipStatus.PARTNER)
        );
    }

    @Test
    void doesNotExposeFormerPartnerPlacesWithoutActiveCouple() {
        Place place = place(30L);
        MemberPlace mine = memberPlace(1L, place, null, Instant.parse("2026-08-10T00:00:00Z"));
        when(coupleMemberRepository.findByMemberId(1L)).thenReturn(Optional.empty());
        when(memberPlaceRepository.findAllByMemberIdInOrderBySavedAtDesc(List.of(1L)))
                .thenReturn(List.of(mine));

        assertThat(service.getVisiblePlaces(1L)).singleElement().satisfies(saved ->
                assertThat(saved.ownershipStatus()).isEqualTo(PlaceOwnershipStatus.MINE)
        );
        verify(memberPlaceRepository).findAllByMemberIdInOrderBySavedAtDesc(List.of(1L));
    }

    @Test
    void filtersVisiblePlacesByCategoryAndOwnership() {
        Place place = place(40L);
        when(place.getCategoryName()).thenReturn("카페");
        MemberPlace mine = memberPlace(
                1L,
                place,
                null,
                Instant.parse("2026-08-10T00:00:00Z")
        );
        when(coupleMemberRepository.findByMemberId(1L)).thenReturn(Optional.empty());
        when(memberPlaceRepository.findAllByMemberIdInOrderBySavedAtDesc(List.of(1L)))
                .thenReturn(List.of(mine));

        List<MemberPlaceView> result = service.getVisiblePlaces(
                1L,
                DulpickPlaceCategory.CAFE,
                PlaceOwnershipStatus.MINE
        );

        assertThat(result).singleElement().satisfies(saved ->
                assertThat(saved.placeId()).isEqualTo(40L)
        );
    }

    private void connectCouple(Long memberId, Long partnerId) {
        Couple couple = mock(Couple.class);
        ActiveCoupleMember mine = mock(ActiveCoupleMember.class);
        ActiveCoupleMember partner = mock(ActiveCoupleMember.class);
        when(couple.getId()).thenReturn(100L);
        when(mine.getCouple()).thenReturn(couple);
        when(mine.getMemberId()).thenReturn(memberId);
        when(partner.getMemberId()).thenReturn(partnerId);
        when(coupleMemberRepository.findByMemberId(memberId)).thenReturn(Optional.of(mine));
        when(coupleMemberRepository.findAllByCoupleId(100L)).thenReturn(List.of(mine, partner));
    }

    private MemberPlace memberPlace(
            Long memberId,
            Place place,
            String alias,
            Instant savedAt
    ) {
        MemberPlace memberPlace = mock(MemberPlace.class);
        when(memberPlace.getMemberId()).thenReturn(memberId);
        when(memberPlace.getPlace()).thenReturn(place);
        when(memberPlace.getAlias()).thenReturn(alias);
        when(memberPlace.getSavedAt()).thenReturn(savedAt);
        return memberPlace;
    }

    private Place place(Long placeId) {
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(placeId);
        return place;
    }

    private List<Object[]> saveCounts(Long placeId, long count) {
        java.util.ArrayList<Object[]> rows = new java.util.ArrayList<>();
        rows.add(new Object[] {placeId, count});
        return rows;
    }
}
