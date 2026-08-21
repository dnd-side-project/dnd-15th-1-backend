package kr.omong.dulpick.domain.place.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "place_image_enrichment_backlogs")
public class PlaceImageEnrichmentBacklog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "kakao_place_id", nullable = false, length = 80)
    private String kakaoPlaceId;

    @Column(nullable = false, length = 30)
    private String reason;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "first_failed_at", nullable = false)
    private Instant firstFailedAt;

    @Column(name = "last_failed_at", nullable = false)
    private Instant lastFailedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PlaceImageEnrichmentBacklog() {
    }
}
