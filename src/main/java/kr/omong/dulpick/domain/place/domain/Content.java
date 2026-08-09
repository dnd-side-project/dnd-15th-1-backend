package kr.omong.dulpick.domain.place.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "contents")
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "canonical_url", nullable = false, length = 1_000)
    private String canonicalUrl;

    @Column(name = "canonical_url_hash", nullable = false, unique = true, length = 64)
    private String canonicalUrlHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private ContentSourceType sourceType;

    @Column(length = 4_000)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "thumbnail_url", length = 1_000)
    private String thumbnailUrl;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "place_count", nullable = false)
    private int placeCount;

    @Column(name = "source_author_name", length = 255)
    private String sourceAuthorName;

    @Column(name = "source_author_username", length = 255)
    private String sourceAuthorUsername;

    @Column(name = "source_published_on")
    private LocalDate sourcePublishedOn;

    @Column(name = "like_count")
    private Long likeCount;

    @Column(name = "comment_count")
    private Long commentCount;

    @Column(name = "engagement_checked_at")
    private Instant engagementCheckedAt;

    @Column(name = "analyzer_model", length = 100)
    private String analyzerModel;

    @Column(name = "prompt_version", length = 50)
    private String promptVersion;

    @Column(name = "extracted_candidates_json", columnDefinition = "TEXT")
    private String extractedCandidatesJson;

    @Column(name = "analyzed_at")
    private Instant analyzedAt;

    @Column(name = "analysis_content_hash", length = 64)
    private String analysisContentHash;

    @Column(name = "analysis_status", length = 30)
    private String analysisStatus;

    @Column(name = "analysis_started_at")
    private Instant analysisStartedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 30)
    private ContentPublicationStatus publicationStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    protected Content() {
    }

    private Content(
            String canonicalUrl,
            String canonicalUrlHash,
            ContentSourceType sourceType,
            String title,
            String content,
            String thumbnailUrl,
            String contentHash,
            Instant now
    ) {
        this.canonicalUrl = canonicalUrl;
        this.canonicalUrlHash = canonicalUrlHash;
        this.sourceType = sourceType;
        this.title = title;
        this.content = content;
        this.thumbnailUrl = thumbnailUrl;
        this.contentHash = contentHash;
        this.placeCount = 0;
        this.publicationStatus = ContentPublicationStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Content create(
            String canonicalUrl,
            String canonicalUrlHash,
            ContentSourceType sourceType,
            String title,
            String content,
            String thumbnailUrl,
            String contentHash,
            Instant now
    ) {
        return new Content(
                canonicalUrl,
                canonicalUrlHash,
                sourceType,
                title,
                content,
                thumbnailUrl,
                contentHash,
                now
        );
    }

    public void updateMetadata(
            String title,
            String content,
            String thumbnailUrl,
            String contentHash,
            Instant now
    ) {
        this.title = title;
        this.content = content;
        this.thumbnailUrl = thumbnailUrl;
        this.contentHash = contentHash;
        this.updatedAt = now;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void publish(Instant now) {
        this.publicationStatus = ContentPublicationStatus.PUBLIC;
        this.updatedAt = now;
    }

    public void updatePlaceCount(int placeCount) {
        this.placeCount = Math.max(placeCount, 0);
    }

    public void updateSourceMetadata(
            String sourceAuthorName,
            String sourceAuthorUsername,
            LocalDate sourcePublishedOn,
            Long likeCount,
            Long commentCount,
            Instant engagementCheckedAt
    ) {
        this.sourceAuthorName = sourceAuthorName;
        this.sourceAuthorUsername = sourceAuthorUsername;
        this.sourcePublishedOn = sourcePublishedOn;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.engagementCheckedAt = engagementCheckedAt;
    }

    public void updateExtractedAnalysis(
            String contentHash,
            String analyzerModel,
            String promptVersion,
            String extractedCandidatesJson,
            Instant analyzedAt
    ) {
        this.analysisContentHash = contentHash;
        this.analyzerModel = analyzerModel;
        this.promptVersion = promptVersion;
        this.extractedCandidatesJson = extractedCandidatesJson;
        this.analyzedAt = analyzedAt;
        this.analysisStatus = "READY";
        this.analysisStartedAt = null;
    }

    public void startAnalysis(Instant startedAt) {
        this.analysisStatus = "PROCESSING";
        this.analysisStartedAt = startedAt;
    }

    public void failAnalysis() {
        this.analysisStatus = "FAILED";
        this.analysisStartedAt = null;
    }

    public Long getId() {
        return id;
    }

    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    public ContentSourceType getSourceType() {
        return sourceType;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public ContentPublicationStatus getPublicationStatus() {
        return publicationStatus;
    }

    public int getPlaceCount() {
        return placeCount;
    }

    public String getSourceAuthorName() {
        return sourceAuthorName;
    }

    public String getSourceAuthorUsername() {
        return sourceAuthorUsername;
    }

    public LocalDate getSourcePublishedOn() {
        return sourcePublishedOn;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public Long getCommentCount() {
        return commentCount;
    }

    public Instant getEngagementCheckedAt() {
        return engagementCheckedAt;
    }

    public String getAnalyzerModel() {
        return analyzerModel;
    }

    public String getAnalysisContentHash() {
        return analysisContentHash;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public String getExtractedCandidatesJson() {
        return extractedCandidatesJson;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }
}
