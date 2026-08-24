package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceCandidate;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceImageEnrichmentBacklog;
import kr.omong.dulpick.domain.place.domain.PlaceImageEnrichmentBacklogRepository;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import kr.omong.dulpick.domain.place.config.PlaceImageEnrichmentProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PlaceImageEnrichmentServiceTest {

    private final PlaceCandidateRepository candidateRepository =
            mock(PlaceCandidateRepository.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceImageProvider imageProvider = mock(PlaceImageProvider.class);
    private final PlaceImageWriter imageWriter = mock(PlaceImageWriter.class);
    private final PlaceImageStorageService imageStorageService = mock(PlaceImageStorageService.class);
    private final PlaceImageEnrichmentBacklogRepository backlogRepository =
            mock(PlaceImageEnrichmentBacklogRepository.class);
    private final PlaceImageEnrichmentService service = new PlaceImageEnrichmentService(
            candidateRepository,
            placeRepository,
            imageProvider,
            imageWriter,
            imageStorageService,
            backlogRepository,
            new PlaceImageEnrichmentProperties(
                    java.time.Duration.ofMinutes(10),
                    3
            ),
            Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void keepsImportSuccessfulWhenPhotoScrapingFails() {
        PlaceCandidate candidate = mock(PlaceCandidate.class);
        Place place = mock(Place.class);
        when(candidate.getPlaceId()).thenReturn(20L);
        when(place.getId()).thenReturn(20L);
        when(place.getKakaoPlaceId()).thenReturn("610012827");
        when(candidateRepository.findAllByImportIdOrderByIdAsc(1L))
                .thenReturn(List.of(candidate));
        when(placeRepository.findAllById(List.of(20L))).thenReturn(List.of(place));
        when(imageProvider.findImageUrls("610012827"))
                .thenThrow(new IllegalStateException("temporary failure"));

        assertThatCode(() -> service.enrichImportPlaces(1L)).doesNotThrowAnyException();

        verifyNoInteractions(imageWriter);
        verify(backlogRepository).recordFailure(
                20L,
                "610012827",
                "PROVIDER_ERROR",
                Instant.parse("2026-08-10T00:00:00Z")
        );
    }

    @Test
    void skipsPlacesThatAlreadyHaveThumbnail() {
        PlaceCandidate candidate = mock(PlaceCandidate.class);
        Place place = mock(Place.class);
        when(candidate.getPlaceId()).thenReturn(20L);
        when(place.getThumbnailUrl()).thenReturn("https://dulpick.omong.kr/api/v1/place-images/existing");
        when(imageStorageService.isPublicUrl(place.getThumbnailUrl())).thenReturn(true);
        when(placeRepository.findAllById(List.of(20L))).thenReturn(List.of(place));
        when(candidateRepository.findAllByImportIdOrderByIdAsc(1L)).thenReturn(List.of(candidate));

        service.enrichImportPlaces(1L);

        verifyNoInteractions(imageProvider, imageWriter, backlogRepository);
    }

    @Test
    void recordsBacklogWhenPhotosAreUnavailable() {
        PlaceCandidate candidate = mock(PlaceCandidate.class);
        Place place = mock(Place.class);
        when(candidate.getPlaceId()).thenReturn(20L);
        when(place.getId()).thenReturn(20L);
        when(place.getKakaoPlaceId()).thenReturn("610012827");
        when(candidateRepository.findAllByImportIdOrderByIdAsc(1L))
                .thenReturn(List.of(candidate));
        when(placeRepository.findAllById(List.of(20L))).thenReturn(List.of(place));
        when(imageProvider.findImageUrls("610012827")).thenReturn(List.of());

        service.enrichImportPlaces(1L);

        verify(backlogRepository).recordFailure(
                20L,
                "610012827",
                "PHOTO_UNAVAILABLE",
                Instant.parse("2026-08-10T00:00:00Z")
        );
        verifyNoInteractions(imageWriter);
    }

    @Test
    void skipsRetryDuringBacklogCooldown() {
        Place place = mock(Place.class);
        PlaceImageEnrichmentBacklog backlog = mock(PlaceImageEnrichmentBacklog.class);
        when(place.getId()).thenReturn(20L);
        when(place.getThumbnailUrl()).thenReturn(null);
        when(place.getKakaoPlaceId()).thenReturn("610012827");
        when(backlog.getStatus()).thenReturn("PENDING");
        when(backlog.getAttemptCount()).thenReturn(1);
        when(backlog.getLastFailedAt()).thenReturn(Instant.parse("2026-08-09T23:55:00Z"));
        when(placeRepository.findById(20L)).thenReturn(java.util.Optional.of(place));
        when(backlogRepository.findByPlaceId(20L)).thenReturn(java.util.Optional.of(backlog));

        service.enrichPlace(20L);

        verifyNoInteractions(imageProvider, imageWriter);
    }

    @Test
    void stopsRetryAfterBacklogAttemptLimit() {
        Place place = mock(Place.class);
        PlaceImageEnrichmentBacklog backlog = mock(PlaceImageEnrichmentBacklog.class);
        when(place.getId()).thenReturn(20L);
        when(place.getThumbnailUrl()).thenReturn(null);
        when(place.getKakaoPlaceId()).thenReturn("610012827");
        when(backlog.getStatus()).thenReturn("PENDING");
        when(backlog.getAttemptCount()).thenReturn(3);
        when(backlogRepository.findByPlaceId(20L)).thenReturn(java.util.Optional.of(backlog));
        when(placeRepository.findById(20L)).thenReturn(java.util.Optional.of(place));

        service.enrichPlace(20L);

        verifyNoInteractions(imageProvider, imageWriter);
    }
}
