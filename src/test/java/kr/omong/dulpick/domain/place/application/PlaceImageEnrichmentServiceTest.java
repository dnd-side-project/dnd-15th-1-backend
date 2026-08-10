package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceCandidate;
import kr.omong.dulpick.domain.place.domain.PlaceCandidateRepository;
import kr.omong.dulpick.domain.place.domain.PlaceRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PlaceImageEnrichmentServiceTest {

    private final PlaceCandidateRepository candidateRepository =
            mock(PlaceCandidateRepository.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceImageProvider imageProvider = mock(PlaceImageProvider.class);
    private final PlaceImageWriter imageWriter = mock(PlaceImageWriter.class);
    private final PlaceImageEnrichmentService service = new PlaceImageEnrichmentService(
            candidateRepository,
            placeRepository,
            imageProvider,
            imageWriter
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
    }
}
