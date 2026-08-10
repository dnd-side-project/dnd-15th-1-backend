package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.PublicContentNotFoundException;
import kr.omong.dulpick.domain.place.domain.Content;
import kr.omong.dulpick.domain.place.domain.ContentPlace;
import kr.omong.dulpick.domain.place.domain.ContentPlaceRepository;
import kr.omong.dulpick.domain.place.domain.ContentPublicationStatus;
import kr.omong.dulpick.domain.place.domain.ContentRepository;
import kr.omong.dulpick.domain.place.domain.MemberPlace;
import kr.omong.dulpick.domain.place.domain.MemberPlaceRepository;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicContentQueryServiceTest {

    private final ContentRepository contentRepository = mock(ContentRepository.class);
    private final ContentPlaceRepository contentPlaceRepository = mock(ContentPlaceRepository.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final MemberPlaceRepository memberPlaceRepository = mock(MemberPlaceRepository.class);
    private final PublicContentQueryService service = new PublicContentQueryService(
            contentRepository,
            contentPlaceRepository,
            placeRepository,
            memberPlaceRepository
    );

    @Test
    void returnsPublicContentWithPublicPlaceShape() {
        Content content = content(10L);
        ContentPlace relation = mock(ContentPlace.class);
        Place place = place(20L);
        when(contentRepository.findByIdAndPublicationStatus(
                10L,
                ContentPublicationStatus.PUBLIC
        )).thenReturn(Optional.of(content));
        when(contentPlaceRepository.findAllByContentIdIn(List.of(10L)))
                .thenReturn(List.of(relation));
        when(relation.getContentId()).thenReturn(10L);
        when(relation.getPlaceId()).thenReturn(20L);
        when(placeRepository.findAllById(List.of(20L))).thenReturn(List.of(place));
        MemberPlace memberPlace = mock(MemberPlace.class);
        when(memberPlace.getPlace()).thenReturn(place);
        when(memberPlaceRepository.findAllByMemberIdAndPlaceIdIn(1L, List.of(20L)))
                .thenReturn(List.of(memberPlace));

        PublicContentView result = service.findPublicContent(1L, 10L);

        assertThat(result.canonicalUrl()).isEqualTo("https://www.instagram.com/reel/example");
        assertThat(result.places()).singleElement().satisfies(publicPlace -> {
            assertThat(publicPlace.placeId()).isEqualTo(20L);
            assertThat(publicPlace.kakaoPlaceId()).isEqualTo("kakao-place-id");
            assertThat(publicPlace.name()).isEqualTo("밀빛 망원점");
            assertThat(publicPlace.savedByMe()).isTrue();
        });
    }

    @Test
    void capsPageSizeAndUsesStableLatestFirstSort() {
        Content content = content(10L);
        PageRequest expectedPage = PageRequest.of(
                0,
                50,
                Sort.by(Sort.Direction.DESC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
        when(contentRepository.findAllByPublicationStatus(
                ContentPublicationStatus.PUBLIC,
                expectedPage
        )).thenReturn(new PageImpl<>(List.of(content), expectedPage, 1));
        when(contentPlaceRepository.findAllByContentIdIn(List.of(10L))).thenReturn(List.of());
        when(placeRepository.findAllById(List.of())).thenReturn(List.of());

        service.findPublicContents(1L, PageRequest.of(0, 500, Sort.by("title")));

        verify(contentRepository).findAllByPublicationStatus(
                ContentPublicationStatus.PUBLIC,
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
