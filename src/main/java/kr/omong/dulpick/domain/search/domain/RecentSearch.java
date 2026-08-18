package kr.omong.dulpick.domain.search.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "recent_searches",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_recent_searches_member_type_query",
                columnNames = {"member_id", "search_type", "normalized_query"}
        )
)
public class RecentSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "search_type", nullable = false, length = 20)
    private RecentSearchType searchType;

    @Column(nullable = false, length = 200)
    private String keyword;

    @Column(name = "normalized_query", nullable = false, length = 200)
    private String normalizedQuery;

    @Column(name = "searched_at", nullable = false)
    private Instant searchedAt;

    protected RecentSearch() {
    }

    private RecentSearch(
            Long memberId,
            RecentSearchType searchType,
            String keyword,
            String normalizedQuery,
            Instant searchedAt
    ) {
        this.memberId = memberId;
        this.searchType = searchType;
        this.keyword = keyword;
        this.normalizedQuery = normalizedQuery;
        this.searchedAt = searchedAt;
    }

    public static RecentSearch record(
            Long memberId,
            RecentSearchType searchType,
            String keyword,
            String normalizedQuery,
            Instant searchedAt
    ) {
        return new RecentSearch(memberId, searchType, keyword, normalizedQuery, searchedAt);
    }

    public void refresh(String keyword, Instant searchedAt) {
        this.keyword = keyword;
        this.searchedAt = searchedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public RecentSearchType getSearchType() {
        return searchType;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getNormalizedQuery() {
        return normalizedQuery;
    }

    public Instant getSearchedAt() {
        return searchedAt;
    }
}
