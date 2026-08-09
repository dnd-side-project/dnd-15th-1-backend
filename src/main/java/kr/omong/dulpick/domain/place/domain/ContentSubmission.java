package kr.omong.dulpick.domain.place.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "content_submissions")
public class ContentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    protected ContentSubmission() {
    }

    private ContentSubmission(Long contentId, Long memberId, Instant submittedAt) {
        this.contentId = contentId;
        this.memberId = memberId;
        this.submittedAt = submittedAt;
    }

    public static ContentSubmission create(Long contentId, Long memberId, Instant submittedAt) {
        return new ContentSubmission(contentId, memberId, submittedAt);
    }
}
