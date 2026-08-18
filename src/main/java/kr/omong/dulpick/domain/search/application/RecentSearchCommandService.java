package kr.omong.dulpick.domain.search.application;

import kr.omong.dulpick.domain.search.application.exception.RecentSearchNotFoundException;
import kr.omong.dulpick.domain.search.domain.RecentSearch;
import kr.omong.dulpick.domain.search.domain.RecentSearchRepository;
import kr.omong.dulpick.domain.search.domain.RecentSearchType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;

@Service
@EnableConfigurationProperties(RecentSearchProperties.class)
public class RecentSearchCommandService {

    private final RecentSearchRepository recentSearchRepository;
    private final RecentSearchProperties properties;
    private final Clock clock;

    public RecentSearchCommandService(
            RecentSearchRepository recentSearchRepository,
            RecentSearchProperties properties,
            Clock clock
    ) {
        this.recentSearchRepository = recentSearchRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public RecentSearchView record(
            Long memberId,
            RecentSearchType type,
            String rawKeyword
    ) {
        String keyword = normalizeDisplayKeyword(rawKeyword);
        String normalizedQuery = keyword.toLowerCase(Locale.ROOT);
        recentSearchRepository.upsert(
                memberId,
                type.name(),
                keyword,
                normalizedQuery,
                clock.instant()
        );
        recentSearchRepository.deleteOverflow(memberId, type.name(), properties.maxPerType());
        RecentSearch recentSearch = recentSearchRepository
                .findByMemberIdAndSearchTypeAndNormalizedQuery(memberId, type, normalizedQuery)
                .orElseThrow(IllegalStateException::new);
        return RecentSearchView.from(recentSearch);
    }

    @Transactional
    public void delete(Long memberId, Long recentSearchId) {
        RecentSearch recentSearch = recentSearchRepository
                .findByIdAndMemberId(recentSearchId, memberId)
                .orElseThrow(RecentSearchNotFoundException::new);
        recentSearchRepository.delete(recentSearch);
    }

    @Transactional
    public void clear(Long memberId, RecentSearchType type) {
        recentSearchRepository.deleteAllByMemberIdAndSearchType(memberId, type);
    }

    private String normalizeDisplayKeyword(String keyword) {
        return keyword.strip().replaceAll("\\s+", " ");
    }
}
