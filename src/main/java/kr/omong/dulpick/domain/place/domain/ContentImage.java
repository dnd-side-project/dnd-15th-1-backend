package kr.omong.dulpick.domain.place.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "content_images")
public class ContentImage {

    @Id
    @Column(name = "image_key", length = 36)
    private String imageKey;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "source_url", nullable = false, length = 2_000)
    private String sourceUrl;

    @Column(name = "source_url_hash", nullable = false, length = 64)
    private String sourceUrlHash;

    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ContentImage() {
    }

    private ContentImage(
            Long contentId,
            String sourceUrl,
            String sourceUrlHash,
            int displayOrder,
            Instant now
    ) {
        this.imageKey = UUID.randomUUID().toString();
        this.contentId = contentId;
        this.sourceUrl = sourceUrl;
        this.sourceUrlHash = sourceUrlHash;
        this.storageKey = imageKey + ".img";
        this.displayOrder = displayOrder;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static ContentImage create(
            Long contentId,
            String sourceUrl,
            String sourceUrlHash,
            int displayOrder,
            Instant now
    ) {
        return new ContentImage(contentId, sourceUrl, sourceUrlHash, displayOrder, now);
    }

    public static ContentImage createManual(
            Long contentId,
            String sourceUrl,
            String sourceUrlHash,
            int displayOrder,
            Instant now
    ) {
        return create(contentId, sourceUrl, sourceUrlHash, displayOrder, now);
    }

    public void updateDisplayOrder(int displayOrder, Instant now) {
        this.displayOrder = displayOrder;
        this.updatedAt = now;
    }

    public void replaceSource(String sourceUrl, String sourceUrlHash, Instant now) {
        this.sourceUrl = sourceUrl;
        this.sourceUrlHash = sourceUrlHash;
        this.updatedAt = now;
    }

    public void markStored(String contentType, Instant now) {
        this.contentType = contentType;
        this.updatedAt = now;
    }

    public void markStored(String contentType, String contentHash, Instant now) {
        this.contentType = contentType;
        this.contentHash = contentHash;
        this.updatedAt = now;
    }

    public void markContentHash(String contentHash, Instant now) {
        this.contentHash = contentHash;
        this.updatedAt = now;
    }

    public String getImageKey() {
        return imageKey;
    }

    public Long getContentId() {
        return contentId;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getSourceUrlHash() {
        return sourceUrlHash;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public String getContentHash() {
        return contentHash;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
