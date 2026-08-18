package kr.omong.dulpick.domain.search.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RecentSearchRepository extends JpaRepository<RecentSearch, Long> {

    Optional<RecentSearch> findByIdAndMemberId(Long id, Long memberId);

    Optional<RecentSearch> findByMemberIdAndSearchTypeAndNormalizedQuery(
            Long memberId,
            RecentSearchType searchType,
            String normalizedQuery
    );

    Page<RecentSearch> findAllByMemberIdAndSearchType(
            Long memberId,
            RecentSearchType searchType,
            Pageable pageable
    );

    long deleteAllByMemberIdAndSearchType(Long memberId, RecentSearchType searchType);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM recent_searches
            WHERE member_id = :memberId
              AND search_type = :searchType
              AND id IN (
                  SELECT id FROM (
                      SELECT id
                      FROM recent_searches
                      WHERE member_id = :memberId
                        AND search_type = :searchType
                      ORDER BY searched_at DESC, id DESC
                      LIMIT 18446744073709551615 OFFSET :keepCount
                  ) overflow
              )
            """, nativeQuery = true)
    int deleteOverflow(
            @Param("memberId") Long memberId,
            @Param("searchType") String searchType,
            @Param("keepCount") int keepCount
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO recent_searches
                (member_id, search_type, keyword, normalized_query, searched_at)
            VALUES
                (:memberId, :searchType, :keyword, :normalizedQuery, :searchedAt)
            ON DUPLICATE KEY UPDATE
                keyword = :keyword,
                searched_at = :searchedAt
            """, nativeQuery = true)
    void upsert(
            @Param("memberId") Long memberId,
            @Param("searchType") String searchType,
            @Param("keyword") String keyword,
            @Param("normalizedQuery") String normalizedQuery,
            @Param("searchedAt") Instant searchedAt
    );
}
