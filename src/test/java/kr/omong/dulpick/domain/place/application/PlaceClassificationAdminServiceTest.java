package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.application.exception.EmptyPlaceClassificationUpdateException;
import kr.omong.dulpick.domain.place.application.exception.PlaceNotFoundException;
import kr.omong.dulpick.domain.place.domain.ClassificationSource;
import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceClassification;
import kr.omong.dulpick.domain.place.domain.PlaceClassificationRepository;
import kr.omong.dulpick.domain.place.domain.PlaceClassificationStatus;
import kr.omong.dulpick.domain.place.domain.PlaceEnvironment;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.domain.place.presentation.dto.request.UpdatePlaceClassificationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaceClassificationAdminServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-18T00:00:00Z");

    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceClassificationRepository placeClassificationRepository =
            mock(PlaceClassificationRepository.class);
    private final PlaceClassificationAdminService service = new PlaceClassificationAdminService(
            placeRepository,
            placeClassificationRepository,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void createsManualClassificationWithoutChangingUnspecifiedAxes() {
        Place place = place();
        when(placeRepository.findById(10L)).thenReturn(Optional.of(place));
        when(placeClassificationRepository.findById(10L)).thenReturn(Optional.empty());
        when(placeClassificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePlaceClassificationRequest request = new UpdatePlaceClassificationRequest();
        request.setEnvironment(PlaceEnvironment.INDOOR);

        PlaceClassificationAdminView view = service.update(10L, request);

        assertThat(view.status()).isEqualTo(PlaceClassificationStatus.PARTIALLY_CLASSIFIED);
        assertThat(view.environment().value()).isEqualTo(PlaceEnvironment.INDOOR);
        assertThat(view.environment().source()).isEqualTo(ClassificationSource.MANUAL);
        assertThat(view.activity().value()).isNull();
        verify(placeClassificationRepository).save(any(PlaceClassification.class));
    }

    @Test
    void rejectsEmptyUpdate() {
        assertThatThrownBy(() -> service.update(10L, new UpdatePlaceClassificationRequest()))
                .isInstanceOf(EmptyPlaceClassificationUpdateException.class);
    }

    @Test
    void listsUnclassifiedPlacesFirst() {
        Place place = place();
        PageRequest pageable = PageRequest.of(0, 20);
        when(placeRepository.searchForClassificationAdmin(
                eq(true),
                eq(""),
                eq(false),
                eq(true),
                eq(false),
                eq(false),
                any()
        )).thenReturn(new PageImpl<>(List.of(place), pageable, 1));
        when(placeClassificationRepository.findAllById(List.of(10L))).thenReturn(List.of());
        when(placeRepository.searchForClassificationAdmin(
                eq(true),
                eq(""),
                eq(true),
                eq(false),
                eq(false),
                eq(false),
                eq(PageRequest.of(0, 1))
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 3));
        when(placeRepository.searchForClassificationAdmin(
                eq(true),
                eq(""),
                eq(false),
                eq(true),
                eq(false),
                eq(false),
                eq(PageRequest.of(0, 1))
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 1));
        when(placeRepository.searchForClassificationAdmin(
                eq(true),
                eq(""),
                eq(false),
                eq(false),
                eq(true),
                eq(false),
                eq(PageRequest.of(0, 1))
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 1));
        when(placeRepository.searchForClassificationAdmin(
                eq(true),
                eq(""),
                eq(false),
                eq(false),
                eq(false),
                eq(true),
                eq(PageRequest.of(0, 1))
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 1));

        PlaceClassificationAdminPage page = service.list(
                PlaceClassificationStatus.UNCLASSIFIED,
                "  ",
                pageable
        );

        assertThat(page.places()).singleElement().satisfies(view -> {
            assertThat(view.placeId()).isEqualTo(10L);
            assertThat(view.status()).isEqualTo(PlaceClassificationStatus.UNCLASSIFIED);
        });
        assertThat(page.counts().all()).isEqualTo(3);
        assertThat(page.counts().unclassified()).isEqualTo(1);
    }

    @Test
    void throwsWhenPlaceMissing() {
        when(placeRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(PlaceNotFoundException.class);
    }

    private Place place() {
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(10L);
        when(place.getKakaoPlaceId()).thenReturn("kakao-10");
        when(place.getName()).thenReturn("성수 카페");
        when(place.getAddress()).thenReturn("서울특별시 성동구 성수동1가");
        when(place.getRoadAddress()).thenReturn("서울특별시 성동구 성수이로");
        when(place.getCategoryName()).thenReturn("카페");
        return place;
    }
}
