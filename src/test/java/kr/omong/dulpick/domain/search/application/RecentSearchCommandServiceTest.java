package kr.omong.dulpick.domain.search.application;

import kr.omong.dulpick.domain.search.application.exception.RecentSearchNotFoundException;
import kr.omong.dulpick.domain.search.domain.RecentSearch;
import kr.omong.dulpick.domain.search.domain.RecentSearchRepository;
import kr.omong.dulpick.domain.search.domain.RecentSearchType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecentSearchCommandServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-18T00:00:00Z");

    private final RecentSearchRepository repository = mock(RecentSearchRepository.class);
    private final RecentSearchCommandService service = new RecentSearchCommandService(
            repository,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void normalizesAndUpsertsKeywordWithinSelectedType() {
        RecentSearch saved = RecentSearch.record(
                1L,
                RecentSearchType.PLACE,
                "성수동 카페",
                "성수동 카페",
                NOW
        );
        when(repository.findByMemberIdAndSearchTypeAndNormalizedQuery(
                1L,
                RecentSearchType.PLACE,
                "성수동 카페"
        )).thenReturn(Optional.of(saved));

        RecentSearchView result = service.record(
                1L,
                RecentSearchType.PLACE,
                "  성수동   카페  "
        );

        assertThat(result.type()).isEqualTo(RecentSearchType.PLACE);
        assertThat(result.keyword()).isEqualTo("성수동 카페");
        verify(repository).upsert(
                1L,
                "PLACE",
                "성수동 카페",
                "성수동 카페",
                NOW
        );
    }

    @Test
    void keepsContentAndPlaceQueriesSeparatedByType() {
        RecentSearch content = RecentSearch.record(
                1L,
                RecentSearchType.CONTENT,
                "Seoul",
                "seoul",
                NOW
        );
        when(repository.findByMemberIdAndSearchTypeAndNormalizedQuery(
                1L,
                RecentSearchType.CONTENT,
                "seoul"
        )).thenReturn(Optional.of(content));

        service.record(1L, RecentSearchType.CONTENT, "Seoul");

        verify(repository).upsert(1L, "CONTENT", "Seoul", "seoul", NOW);
    }

    @Test
    void rejectsDeletingAnotherMembersHistory() {
        when(repository.findByIdAndMemberId(100L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, 100L))
                .isInstanceOf(RecentSearchNotFoundException.class);
    }
}
