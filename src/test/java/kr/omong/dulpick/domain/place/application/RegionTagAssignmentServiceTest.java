package kr.omong.dulpick.domain.place.application;

import kr.omong.dulpick.domain.place.domain.Place;
import kr.omong.dulpick.domain.place.domain.PlaceRegionTagRepository;
import kr.omong.dulpick.domain.place.domain.RegionTag;
import kr.omong.dulpick.domain.place.domain.RegionTagRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegionTagAssignmentServiceTest {

    @Test
    void linksOnlyTagsWhoseNamesAppearInAddress() {
        RegionTagRepository tagRepository = mock(RegionTagRepository.class);
        PlaceRegionTagRepository linkRepository = mock(PlaceRegionTagRepository.class);
        RegionTagAssignmentService service = new RegionTagAssignmentService(
                tagRepository,
                linkRepository
        );
        RegionTag seongsu = mock(RegionTag.class);
        RegionTag gangnam = mock(RegionTag.class);
        when(seongsu.getId()).thenReturn(1L);
        when(seongsu.getName()).thenReturn("성수");
        when(gangnam.getId()).thenReturn(2L);
        when(gangnam.getName()).thenReturn("강남");
        when(tagRepository.findAllByActiveTrueOrderByDisplayOrderAscIdAsc())
                .thenReturn(List.of(seongsu, gangnam));
        Place place = mock(Place.class);
        when(place.getId()).thenReturn(10L);
        when(place.getAddress()).thenReturn("서울특별시 성동구 성수동1가");
        when(place.getRoadAddress()).thenReturn("서울특별시 성동구 서울숲길");
        Instant now = Instant.parse("2026-08-18T00:00:00Z");

        service.assignMatchingTags(place, now);

        verify(linkRepository).insertIfAbsent(10L, 1L, now);
        verify(linkRepository, never()).insertIfAbsent(10L, 2L, now);
    }
}
