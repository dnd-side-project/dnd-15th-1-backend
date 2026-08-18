package kr.omong.dulpick.domain.search.application;

import kr.omong.dulpick.domain.search.domain.RecentSearch;
import kr.omong.dulpick.domain.search.domain.RecentSearchType;

import java.time.Instant;

public record RecentSearchView(
        Long recentSearchId,
        RecentSearchType type,
        String keyword,
        Instant searchedAt
) {

    public static RecentSearchView from(RecentSearch recentSearch) {
        return new RecentSearchView(
                recentSearch.getId(),
                recentSearch.getSearchType(),
                recentSearch.getKeyword(),
                recentSearch.getSearchedAt()
        );
    }
}
