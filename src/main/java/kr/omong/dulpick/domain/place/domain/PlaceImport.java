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
@Table(name = "place_imports")
public class PlaceImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "content_id")
    private Long contentId;

    @Column(name = "canonical_url", nullable = false, length = 1_000)
    private String canonicalUrl;

    @Column(name = "canonical_url_hash", nullable = false, length = 64)
    private String canonicalUrlHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private ContentSourceType sourceType;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "source_updated_at")
    private Instant sourceUpdatedAt;

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

    @Column(length = 4_000)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "thumbnail_url", length = 1_000)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PlaceImportStatus status;

    @Column(name = "processing_claim_token", length = 36)
    private String processingClaimToken;

    @Column(name = "failure_code", length = 80)
    private String failureCode;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected PlaceImport() {
    }

    private PlaceImport(
            Long memberId,
            String canonicalUrl,
            String canonicalUrlHash,
            ContentSourceType sourceType,
            Instant createdAt
    ) {
        this.memberId = memberId;
        this.canonicalUrl = canonicalUrl;
        this.canonicalUrlHash = canonicalUrlHash;
        this.sourceType = sourceType;
        this.status = PlaceImportStatus.RECEIVED;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static PlaceImport receive(
            Long memberId,
            String canonicalUrl,
            String canonicalUrlHash,
            ContentSourceType sourceType,
            Instant createdAt
    ) {
        return new PlaceImport(
                memberId,
                canonicalUrl,
                canonicalUrlHash,
                sourceType,
                createdAt
        );
    }

    public void start(Instant now) {
        status = PlaceImportStatus.PROCESSING;
        updatedAt = now;
    }

    public void attachContent(Long contentId) {
        this.contentId = contentId;
    }

    public void retry(Instant now) {
        status = PlaceImportStatus.PROCESSING;
        failureCode = null;
        updatedAt = now;
    }

    public void requeue(Instant now) {
        status = PlaceImportStatus.RECEIVED;
        processingClaimToken = null;
        failureCode = null;
        updatedAt = now;
    }

    public void complete(
            String title,
            String content,
            String thumbnailUrl,
            String contentHash,
            Instant sourceUpdatedAt,
            Instant now
    ) {
        recordMetadata(title, content, thumbnailUrl, contentHash, sourceUpdatedAt);
        this.sourceUpdatedAt = sourceUpdatedAt;
        status = PlaceImportStatus.REVIEW_REQUIRED;
        processingClaimToken = null;
        updatedAt = now;
        completedAt = now;
    }

    public void recordMetadata(
            String title,
            String content,
            String thumbnailUrl,
            String contentHash,
            Instant sourceUpdatedAt
    ) {
        this.title = truncate(title, 4_000);
        this.content = content;
        this.thumbnailUrl = truncate(thumbnailUrl, 1_000);
        this.contentHash = contentHash;
        this.sourceUpdatedAt = sourceUpdatedAt;
    }

    public void recordSourceMetadata(
            String sourceAuthorName,
            String sourceAuthorUsername,
            LocalDate sourcePublishedOn,
            Long likeCount,
            Long commentCount,
            Instant engagementCheckedAt
    ) {
        this.sourceAuthorName = truncate(sourceAuthorName, 255);
        this.sourceAuthorUsername = truncate(sourceAuthorUsername, 255);
        this.sourcePublishedOn = sourcePublishedOn;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.engagementCheckedAt = engagementCheckedAt;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public void fail(String failureCode, Instant now) {
        status = PlaceImportStatus.FAILED;
        processingClaimToken = null;
        this.failureCode = failureCode;
        retryCount++;
        updatedAt = now;
    }

    public void markCompleted(Instant now) {
        if (status == PlaceImportStatus.REVIEW_REQUIRED) {
            status = PlaceImportStatus.COMPLETED;
            updatedAt = now;
        }
    }

    public void adminComplete(Instant now) {
        this.status = PlaceImportStatus.COMPLETED;
        this.failureCode = null;
        this.processingClaimToken = null;
        this.updatedAt = now;
        this.completedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getContentId() {
        return contentId;
    }

    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    public ContentSourceType getSourceType() {
        return sourceType;
    }

    public PlaceImportStatus getStatus() {
        return status;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public String getProcessingClaimToken() {
        return processingClaimToken;
    }

    public String getContentHash() {
        return contentHash;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
