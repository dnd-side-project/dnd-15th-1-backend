package kr.omong.dulpick.domain.search.application;

import kr.omong.dulpick.domain.search.domain.RecentSearchRepository;
import kr.omong.dulpick.domain.search.domain.RecentSearchType;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecentSearchQueryServiceTest {

    @Test
    void capsPageSizeAndForcesLatestFirstOrder() {
        RecentSearchRepository repository = mock(RecentSearchRepository.class);
        RecentSearchQueryService service = new RecentSearchQueryService(repository);
        PageRequest expected = PageRequest.of(
                0,
                50,
                Sort.by(Sort.Direction.DESC, "searchedAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );
        when(repository.findAllByMemberIdAndSearchType(
                1L,
                RecentSearchType.CONTENT,
                expected
        )).thenReturn(new PageImpl<>(List.of(), expected, 0));

        service.getRecentSearches(
                1L,
                RecentSearchType.CONTENT,
                PageRequest.of(0, 500, Sort.by("keyword"))
        );

        verify(repository).findAllByMemberIdAndSearchType(
                1L,
                RecentSearchType.CONTENT,
                expected
        );
    }
}
