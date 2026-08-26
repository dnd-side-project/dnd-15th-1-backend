package kr.omong.dulpick.domain.search.application;

import kr.omong.dulpick.domain.search.domain.RecentSearchRepository;
import kr.omong.dulpick.domain.search.domain.RecentSearchType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecentSearchQueryService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final Sort LATEST_FIRST = Sort.by(Sort.Direction.DESC, "searchedAt")
            .and(Sort.by(Sort.Direction.DESC, "id"));

    private final RecentSearchRepository recentSearchRepository;

    public RecentSearchQueryService(RecentSearchRepository recentSearchRepository) {
        this.recentSearchRepository = recentSearchRepository;
    }

    @Transactional(readOnly = true)
    public Page<RecentSearchView> getRecentSearches(
            Long memberId,
            RecentSearchType type,
            Pageable pageable
    ) {
        Pageable orderedPageable = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
                LATEST_FIRST
        );
        return recentSearchRepository
                .findAllByMemberIdAndSearchType(memberId, type, orderedPageable)
                .map(RecentSearchView::from);
    }
}
