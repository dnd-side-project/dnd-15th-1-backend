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

@Entity
@Table(name = "place_imports")
public class PlaceImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PlaceImportStatus status;

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

    public void retry(Instant now) {
        status = PlaceImportStatus.PROCESSING;
        failureCode = null;
        updatedAt = now;
    }

    public void complete(
            String contentHash,
            Instant sourceUpdatedAt,
            Instant now
    ) {
        this.contentHash = contentHash;
        this.sourceUpdatedAt = sourceUpdatedAt;
        status = PlaceImportStatus.REVIEW_REQUIRED;
        updatedAt = now;
        completedAt = now;
    }

    public void fail(String failureCode, Instant now) {
        status = PlaceImportStatus.FAILED;
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

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
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

    public String getContentHash() {
        return contentHash;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
