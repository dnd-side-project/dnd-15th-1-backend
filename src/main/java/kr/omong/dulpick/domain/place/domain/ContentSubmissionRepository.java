package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentSubmissionRepository extends JpaRepository<ContentSubmission, Long> {

    boolean existsByContentIdAndMemberId(Long contentId, Long memberId);

    @Modifying
    @Query(value = """
            INSERT INTO content_submissions (content_id, member_id, submitted_at)
            VALUES (:contentId, :memberId, :submittedAt)
            ON DUPLICATE KEY UPDATE content_id = content_id
            """, nativeQuery = true)
    void insertIfAbsent(
            @Param("contentId") Long contentId,
            @Param("memberId") Long memberId,
            @Param("submittedAt") java.time.Instant submittedAt
    );
}
