package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.ClassificationSource;
import kr.omong.dulpick.domain.place.domain.PlaceClassification;
import kr.omong.dulpick.domain.place.domain.PlaceClassificationRepository;
import kr.omong.dulpick.domain.place.domain.PlaceClassificationStatus;
import kr.omong.dulpick.domain.place.domain.PlaceEnvironment;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaceDateTraitsReaderTest {

    private final PlaceClassificationRepository repository = mock(PlaceClassificationRepository.class);
    private final PlaceDateTraitsReader reader = new PlaceDateTraitsReader(repository);

    @Test
    void loadsTraitsForAllPlacesInOneQueryAndSkipsMissingOnes() {
        PlaceClassification classification = PlaceClassification.initialize(1L, Instant.now());
        classification.classifyEnvironment(PlaceEnvironment.INDOOR, ClassificationSource.AI, Instant.now());
        when(repository.findAllById(List.of(1L, 2L))).thenReturn(List.of(classification));

        Map<Long, PlaceDateTraitsView> traits = reader.findByPlaceIds(Arrays.asList(1L, 2L, 1L, null));

        assertThat(traits).containsOnlyKeys(1L);
        assertThat(traits.get(1L).environment()).isEqualTo(PlaceEnvironment.INDOOR);
        verify(repository).findAllById(List.of(1L, 2L));
    }

    @Test
    void returnsEmptyWithoutQueryingWhenThereAreNoPlaceIds() {
        assertThat(reader.findByPlaceIds(List.of())).isEmpty();
        assertThat(reader.findByPlaceIds(null)).isEmpty();
        verify(repository, never()).findAllById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsUnclassifiedWhenSinglePlaceHasNoClassification() {
        when(repository.findById(3L)).thenReturn(Optional.empty());

        assertThat(reader.findByPlaceId(3L).status()).isEqualTo(PlaceClassificationStatus.UNCLASSIFIED);
        assertThat(reader.findByPlaceId(null).status()).isEqualTo(PlaceClassificationStatus.UNCLASSIFIED);
    }
}
