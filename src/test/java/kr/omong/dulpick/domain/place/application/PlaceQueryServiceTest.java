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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaceQueryServiceTest {

    private final MemberPlaceRepository memberPlaceRepository = mock(MemberPlaceRepository.class);
    private final ActiveCoupleMemberRepository coupleMemberRepository =
            mock(ActiveCoupleMemberRepository.class);
    private final RegionTagQueryService regionTagQueryService = mock(RegionTagQueryService.class);
    private final PlaceQueryService service = new PlaceQueryService(
            memberPlaceRepository,
            coupleMemberRepository,
            regionTagQueryService
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
    void marksPartnerOnlyPlaceAsTogetherWhenCoupleIsActive() {
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
                assertThat(saved.ownershipStatus()).isEqualTo(PlaceOwnershipStatus.TOGETHER)
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
    void filtersVisiblePlacesByCategoryOwnershipAndRegionTag() {
        Place place = place(40L);
        when(place.getCategoryName()).thenReturn("카페");
        MemberPlace mine = memberPlace(
                1L,
                place,
                null,
                Instant.parse("2026-08-10T00:00:00Z")
        );
        RegionTagSummaryView seongsu = new RegionTagSummaryView(1L, "성수", 1);
        when(coupleMemberRepository.findByMemberId(1L)).thenReturn(Optional.empty());
        when(memberPlaceRepository.findAllByMemberIdInOrderBySavedAtDesc(List.of(1L)))
                .thenReturn(List.of(mine));
        when(regionTagQueryService.getSummary(1L)).thenReturn(seongsu);
        when(regionTagQueryService.getTagsByPlaceIds(List.of(40L)))
                .thenReturn(Map.of(40L, List.of(seongsu)));

        List<MemberPlaceView> result = service.getVisiblePlaces(
                1L,
                DulpickPlaceCategory.CAFE,
                PlaceOwnershipStatus.MINE,
                1L
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
}
