package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.member.domain.DatePreferenceOption;
import kr.omong.dulpick.domain.member.domain.DatePreferences;
import kr.omong.dulpick.domain.member.domain.MemberProfile;
import kr.omong.dulpick.domain.member.domain.MemberProfileRepository;
import kr.omong.dulpick.domain.place.application.exception.PlaceNotFoundException;
import kr.omong.dulpick.domain.place.application.exception.PublicContentNotFoundException;
import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentImageRepository;
import kr.omong.dulpick.domain.place.domain.ContentPlace;
import kr.omong.dulpick.domain.place.domain.ContentPlaceRepository;
import kr.omong.dulpick.domain.place.domain.ContentPublicationStatus;
import kr.omong.dulpick.domain.place.domain.ContentRecommendationSort;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.MemberPlace;
import kr.omong.dulpick.domain.place.domain.MemberPlaceRepository;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceClassification;
import kr.omong.dulpick.domain.place.domain.PlaceClassificationRepository;
import kr.omong.dulpick.domain.place.domain.PlaceClassificationStatus;
import kr.omong.dulpick.domain.place.domain.PlaceEnvironment;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicContentQueryServiceTest {

    private final ContentRepository contentRepository = mock(ContentRepository.class);
    private final ContentPlaceRepository contentPlaceRepository = mock(ContentPlaceRepository.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceClassificationRepository placeClassificationRepository =
            mock(PlaceClassificationRepository.class);
    private final MemberPlaceRepository memberPlaceRepository = mock(MemberPlaceRepository.class);
    private final MemberProfileRepository memberProfileRepository = mock(MemberProfileRepository.class);
    private final ContentImageRepository contentImageRepository = mock(ContentImageRepository.class);
    private final PublicContentQueryService service = new PublicContentQueryService(
            contentRepository,
            contentPlaceRepository,
            placeRepository,
            placeClassificationRepository,
            memberPlaceRepository,
            memberProfileRepository,
            contentImageRepository
    );

    @Test
    void returnsPublicContentWithPublicPlaceShape() {
        Content content = content(10L);
        ContentPlace relation = contentPlace(10L, 20L);
        Place place = place(20L);
        when(contentRepository.findByIdAndPublicationStatus(
                10L,
                ContentPublicationStatus.PUBLIC
        )).thenReturn(Optional.of(content));
        stubPlaces(List.of(relation), List.of(place));
        when(placeClassificationRepository.findAllById(anyList())).thenReturn(List.of());
        MemberPlace mine = mock(MemberPlace.class);
        when(mine.getPlace()).thenReturn(place);
        when(memberPlaceRepository.findAllByMemberIdAndPlaceIdIn(eq(1L), anyList()))
                .thenReturn(List.of(mine));
        when(memberPlaceRepository.countSavesByPlaceIdIn(anyList()))
                .thenReturn(saveCounts(row(20L, 1L)));

        PublicContentView result = service.findPublicContent(1L, 10L);

        assertThat(result.canonicalUrl()).isEqualTo("https://www.instagram.com/reel/example");
        assertThat(result.places()).singleElement().satisfies(publicPlace -> {
            assertThat(publicPlace.placeId()).isEqualTo(20L);
            assertThat(publicPlace.kakaoPlaceId()).isEqualTo("kakao-place-id");
            assertThat(publicPlace.name()).isEqualTo("밀빛 망원점");
            assertThat(publicPlace.savedByMe()).isTrue();
            assertThat(publicPlace.dateTraits().status())
                    .isEqualTo(PlaceClassificationStatus.UNCLASSIFIED);
        });
    }

    @Test
    void ranksContentsBySaveCountDescendingByDefault() {
        Content popular = content(10L);
        Content unpopular = content(11L);
        Place popularPlace = place(20L);
        Place unpopularPlace = place(21L);
        ContentPlace popularRelation = contentPlace(10L, 20L);
        ContentPlace unpopularRelation = contentPlace(11L, 21L);
        when(contentRepository.findAllByPublicationStatusOrderByCreatedAtDesc(
                ContentPublicationStatus.PUBLIC
        )).thenReturn(List.of(unpopular, popular));
        stubPlaces(
                List.of(popularRelation, unpopularRelation),
                List.of(popularPlace, unpopularPlace)
        );
        when(placeClassificationRepository.findAllById(anyList())).thenReturn(List.of());
        when(memberPlaceRepository.countSavesByPlaceIdIn(anyList()))
                .thenReturn(saveCounts(row(20L, 5L), row(21L, 1L)));
        when(memberPlaceRepository.findAllByMemberIdAndPlaceIdIn(eq(1L), anyList()))
                .thenReturn(List.of());

        Page<PublicContentView> result = service.findPublicContents(
                1L,
                PageRequest.of(0, 20),
                ContentRecommendationSort.POPULAR
        );

        assertThat(result.getContent()).extracting(PublicContentView::contentId)
                .containsExactly(10L, 11L);
    }

    @Test
    void ranksPreferenceMatchesFirstThenSaveCountDescending() {
        Content weakerMatch = content(10L);
        Content strongerMatch = content(11L);
        Place weakerPlace = place(20L);
        Place strongerPlace = place(21L);
        when(contentRepository.findAllByPublicationStatusOrderByCreatedAtDesc(
                ContentPublicationStatus.PUBLIC
        )).thenReturn(List.of(weakerMatch, strongerMatch));
        stubPlaces(
                List.of(contentPlace(10L, 20L), contentPlace(11L, 21L)),
                List.of(weakerPlace, strongerPlace)
        );
        PlaceClassification weaker = mock(PlaceClassification.class);
        PlaceClassification stronger = mock(PlaceClassification.class);
        when(weaker.getPlaceId()).thenReturn(20L);
        when(weaker.getStatus()).thenReturn(PlaceClassificationStatus.CLASSIFIED);
        when(weaker.getEnvironment()).thenReturn(PlaceEnvironment.INDOOR);
        when(stronger.getPlaceId()).thenReturn(21L);
        when(stronger.getStatus()).thenReturn(PlaceClassificationStatus.CLASSIFIED);
        when(stronger.getEnvironment()).thenReturn(PlaceEnvironment.INDOOR);
        when(stronger.getActivity()).thenReturn(kr.omong.dulpick.domain.place.domain.PlaceActivity.ACTIVE);
        when(placeClassificationRepository.findAllById(anyList()))
                .thenReturn(List.of(weaker, stronger));
        when(memberPlaceRepository.countSavesByPlaceIdIn(anyList()))
                .thenReturn(saveCounts(row(20L, 9L), row(21L, 1L)));
        when(memberPlaceRepository.findAllByMemberIdAndPlaceIdIn(eq(1L), anyList()))
                .thenReturn(List.of());
        MemberProfile profile = mock(MemberProfile.class);
        when(profile.getDatePreferences()).thenReturn(new DatePreferences(
                DatePreferenceOption.INDOOR,
                DatePreferenceOption.ACTIVE,
                DatePreferenceOption.DAY,
                DatePreferenceOption.FOOD
        ));
        when(memberProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        Page<PublicContentView> result = service.findPublicContents(
                1L,
                PageRequest.of(0, 20),
                ContentRecommendationSort.PREFERENCE
        );

        assertThat(result.getContent()).extracting(PublicContentView::contentId)
                .containsExactly(11L, 10L);
        assertThat(result.getContent().getFirst().places()).extracting(PublicPlaceView::placeId)
                .containsExactly(21L);
    }

    @Test
    void doesNotReturnInstagramReelAndPostAsSeparatePublicContents() {
        Content reel = content(10L);
        Content post = content(11L);
        when(reel.getSourceType()).thenReturn(ContentSourceType.INSTAGRAM_REEL);
        when(reel.getCanonicalUrl()).thenReturn("https://www.instagram.com/reel/DcNY1IPT5Yg");
        when(post.getSourceType()).thenReturn(ContentSourceType.INSTAGRAM_POST);
        when(post.getCanonicalUrl()).thenReturn("https://www.instagram.com/p/DcNY1IPT5Yg");
        when(post.getPlaceCount()).thenReturn(2);
        when(contentRepository.findAllByPublicationStatusOrderByCreatedAtDesc(
                ContentPublicationStatus.PUBLIC
        )).thenReturn(List.of(reel, post));
        stubPlaces(List.of(contentPlace(11L, 20L)), List.of(place(20L)));
        when(placeClassificationRepository.findAllById(anyList())).thenReturn(List.of());
        when(memberPlaceRepository.countSavesByPlaceIdIn(anyList()))
                .thenReturn(saveCounts(row(20L, 1L)));
        when(memberPlaceRepository.findAllByMemberIdAndPlaceIdIn(eq(1L), anyList()))
                .thenReturn(List.of());

        Page<PublicContentView> result = service.findPublicContents(
                1L,
                PageRequest.of(0, 20),
                ContentRecommendationSort.POPULAR
        );

        assertThat(result.getContent()).extracting(PublicContentView::contentId)
                .containsExactly(11L);
    }

    @Test
    void doesNotReturnInstagramReelAndPostTwiceInSearchResults() {
        Content reel = content(10L);
        Content post = content(11L);
        when(reel.getSourceType()).thenReturn(ContentSourceType.INSTAGRAM_REEL);
        when(reel.getCanonicalUrl()).thenReturn("https://www.instagram.com/reel/DcNY1IPT5Yg");
        when(post.getSourceType()).thenReturn(ContentSourceType.INSTAGRAM_POST);
        when(post.getCanonicalUrl()).thenReturn("https://www.instagram.com/p/DcNY1IPT5Yg");
        PageRequest pageable = PageRequest.of(0, 20);
        when(contentRepository.searchByPublicationStatusAndKeyword(
                ContentPublicationStatus.PUBLIC.name(),
                "+데이트",
                pageable
        )).thenReturn(new PageImpl<>(List.of(reel, post), pageable, 2));
        when(contentPlaceRepository.findAllByContentIdIn(List.of(11L))).thenReturn(List.of());
        when(placeRepository.findAllById(List.of())).thenReturn(List.of());

        Page<PublicContentView> result = service.searchPublicContents(
                1L,
                " 데이트 ",
                pageable
        );

        assertThat(result.getContent()).extracting(PublicContentView::contentId)
                .containsExactly(11L);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void searchesOnlyPublicContentTitleAndBodyWithStablePaging() {
        Content content = content(10L);
        PageRequest expectedPage = PageRequest.of(0, 20);
        when(contentRepository.searchByPublicationStatusAndKeyword(
                ContentPublicationStatus.PUBLIC.name(),
                "+서울 +데이트",
                expectedPage
        )).thenReturn(new PageImpl<>(List.of(content), expectedPage, 1));
        when(contentPlaceRepository.findAllByContentIdIn(List.of(10L))).thenReturn(List.of());
        when(placeRepository.findAllById(List.of())).thenReturn(List.of());

        service.searchPublicContents(
                1L,
                "  서울 데이트  ",
                PageRequest.of(0, 20)
        );

        verify(contentRepository).searchByPublicationStatusAndKeyword(
                ContentPublicationStatus.PUBLIC.name(),
                "+서울 +데이트",
                expectedPage
        );
    }

    @Test
    void hidesPendingContentFromSingleContentLookup() {
        when(contentRepository.findByIdAndPublicationStatus(
                10L,
                ContentPublicationStatus.PUBLIC
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findPublicContent(1L, 10L))
                .isInstanceOf(PublicContentNotFoundException.class);
    }

    @Test
    void returnsOnlyPublicContentsLinkedToPlace() {
        Content linked = content(10L);
        when(placeRepository.existsById(20L)).thenReturn(true);
        PageRequest pageable = PageRequest.of(0, 20);
        when(contentRepository.findAllByPlaceIdAndPublicationStatus(
                eq(20L),
                eq(ContentPublicationStatus.PUBLIC),
                any()
        )).thenReturn(new PageImpl<>(List.of(linked), pageable, 1));
        stubPlaces(List.of(contentPlace(10L, 20L)), List.of(place(20L)));
        when(placeClassificationRepository.findAllById(anyList())).thenReturn(List.of());
        when(memberPlaceRepository.countSavesByPlaceIdIn(anyList()))
                .thenReturn(saveCounts(row(20L, 1L)));
        when(memberPlaceRepository.findAllByMemberIdAndPlaceIdIn(eq(1L), anyList()))
                .thenReturn(List.of());

        Page<PublicContentView> result = service.findPublicContentsByPlaceId(
                1L,
                20L,
                pageable
        );

        assertThat(result.getContent()).extracting(PublicContentView::contentId)
                .containsExactly(10L);
    }

    @Test
    void rejectsUnknownPlaceWhenListingLinkedContents() {
        when(placeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.findPublicContentsByPlaceId(
                1L,
                99L,
                PageRequest.of(0, 20)
        )).isInstanceOf(PlaceNotFoundException.class);
    }

    private void stubPlaces(List<ContentPlace> relations, List<Place> places) {
        when(contentPlaceRepository.findAllByContentIdIn(anyList())).thenReturn(relations);
        when(placeRepository.findAllById(anyList())).thenReturn(places);
        ContentPlace first = relations.getFirst();
        if (first.getContentId() == null) {
            when(first.getContentId()).thenReturn(10L);
            when(first.getPlaceId()).thenReturn(places.getFirst().getId());
        }
    }

    private List<Object[]> saveCounts(Object[]... rows) {
        return Arrays.asList(rows);
    }

    private Object[] row(Long placeId, long saveCount) {
        return new Object[] {placeId, saveCount};
    }

    private ContentPlace contentPlace(Long contentId, Long placeId) {
        ContentPlace relation = mock(ContentPlace.class);
        when(relation.getContentId()).thenReturn(contentId);
        when(relation.getPlaceId()).thenReturn(placeId);
        return relation;
    }

    private Content content(Long id) {
        Content content = mock(Content.class);
        when(content.getId()).thenReturn(id);
        when(content.getCanonicalUrl())
                .thenReturn("https://www.instagram.com/reel/example");
        when(content.getPlaceCount()).thenReturn(1);
        return content;
    }

    private Place place(Long id) {
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(id);
        when(place.getKakaoPlaceId()).thenReturn("kakao-place-id");
        when(place.getName()).thenReturn("밀빛 망원점");
        when(place.getAddress()).thenReturn("서울 마포구 망원동");
        when(place.getRoadAddress()).thenReturn("서울 마포구 희우정로");
        when(place.getLatitude()).thenReturn(new BigDecimal("37.5546637"));
        when(place.getLongitude()).thenReturn(new BigDecimal("126.9033951"));
        return place;
    }
}
