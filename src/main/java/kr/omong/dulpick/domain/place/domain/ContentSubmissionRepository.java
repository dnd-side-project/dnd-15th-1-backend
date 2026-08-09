package kr.omong.dulpick.domain.place.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentSubmissionRepository extends JpaRepository<ContentSubmission, Long> {

    boolean existsByContentIdAndMemberId(Long contentId, Long memberId);
}
