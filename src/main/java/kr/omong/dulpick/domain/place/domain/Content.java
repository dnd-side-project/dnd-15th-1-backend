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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }
}
