package kr.omong.dulpick.domain.place.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "place_images")
public class PlaceImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "image_url", nullable = false, length = 2_000)
    private String imageUrl;

    @Column(name = "image_url_hash", nullable = false, length = 64)
    private String imageUrlHash;

    @Column(name = "storage_key", length = 36, unique = true)
    private String storageKey;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "source_provider", nullable = false, length = 30)
    private String sourceProvider;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PlaceImage() {
    }

    private PlaceImage(
            Long placeId,
            String imageUrl,
            String imageUrlHash,
            int displayOrder,
            String sourceProvider,
            Instant createdAt
    ) {
        this.placeId = placeId;
        this.imageUrl = imageUrl;
        this.imageUrlHash = imageUrlHash;
        this.displayOrder = displayOrder;
        this.sourceProvider = sourceProvider;
        this.createdAt = createdAt;
    }

    public static PlaceImage create(
            Long placeId,
            String imageUrl,
            String imageUrlHash,
            int displayOrder,
            Instant createdAt
    ) {
        return new PlaceImage(
                placeId,
                imageUrl,
                imageUrlHash,
                displayOrder,
                "KAKAO_MAP_SCRAPE",
                createdAt
        );
    }

    public static PlaceImage createStored(
            Long placeId,
            String imageUrl,
            String imageUrlHash,
            String storageKey,
            String contentType,
            int displayOrder,
            Instant createdAt
    ) {
        PlaceImage image = new PlaceImage(
                placeId,
                imageUrl,
                imageUrlHash,
                displayOrder,
                "KAKAO_MAP_SCRAPE",
                createdAt
        );
        image.storageKey = storageKey;
        image.contentType = contentType;
        return image;
    }

    public static PlaceImage createManualStored(
            Long placeId,
            String imageUrl,
            String imageUrlHash,
            String storageKey,
            String contentType,
            int displayOrder,
            Instant createdAt
    ) {
        PlaceImage image = createStored(
                placeId,
                imageUrl,
                imageUrlHash,
                storageKey,
                contentType,
                displayOrder,
                createdAt
        );
        image.sourceProvider = "OPS_UPLOAD";
        return image;
    }

    public Long getId() {
        return id;
    }

    public Long getPlaceId() {
        return placeId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void updateDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
}
