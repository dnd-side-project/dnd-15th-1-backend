package kr.omong.dulpick.domain.place.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "content_image_enrichment_backlogs")
public class ContentImageEnrichmentBacklog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "source_urls", nullable = false, columnDefinition = "TEXT")
    private String sourceUrls;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ContentImageEnrichmentBacklog() {
    }

    public Long getContentId() {
        return contentId;
    }

    public String getSourceUrls() {
        return sourceUrls;
    }
}
