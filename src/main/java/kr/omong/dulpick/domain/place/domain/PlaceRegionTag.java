package kr.omong.dulpick.domain.place.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "place_region_tags",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_place_region_tags_place_tag",
                columnNames = {"place_id", "region_tag_id"}
        )
)
public class PlaceRegionTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "region_tag_id", nullable = false)
    private Long regionTagId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PlaceRegionTag() {
    }

    private PlaceRegionTag(Long placeId, Long regionTagId, Instant createdAt) {
        this.placeId = placeId;
        this.regionTagId = regionTagId;
        this.createdAt = createdAt;
    }

    public static PlaceRegionTag link(Long placeId, Long regionTagId, Instant createdAt) {
        return new PlaceRegionTag(placeId, regionTagId, createdAt);
    }

    public Long getId() {
        return id;
    }

    public Long getPlaceId() {
        return placeId;
    }

    public Long getRegionTagId() {
        return regionTagId;
    }
}
