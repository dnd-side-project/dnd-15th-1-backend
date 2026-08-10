package kr.omong.dulpick.domain.place.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "content_places")
public class ContentPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ContentPlace() {
    }

    private ContentPlace(Long contentId, Long placeId, Instant createdAt) {
        this.contentId = contentId;
        this.placeId = placeId;
        this.createdAt = createdAt;
    }

    public static ContentPlace create(Long contentId, Long placeId, Instant createdAt) {
        return new ContentPlace(contentId, placeId, createdAt);
    }

    public Long getPlaceId() {
        return placeId;
    }

    public Long getContentId() {
        return contentId;
    }
}
