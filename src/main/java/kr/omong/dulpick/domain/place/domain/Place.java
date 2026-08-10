package kr.omong.dulpick.domain.place.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "places")
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kakao_place_id", nullable = false, unique = true, length = 80)
    private String kakaoPlaceId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(name = "road_address", length = 500)
    private String roadAddress;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 100)
    private String category;

    @Column(name = "category_group_code", length = 3)
    private String categoryGroupCode;

    @Column(name = "thumbnail_url", length = 1_000)
    private String thumbnailUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Place() {
    }

    private Place(
            String kakaoPlaceId,
            String name,
            String address,
            String roadAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            String category,
            String categoryGroupCode,
            String thumbnailUrl,
            Instant now
    ) {
        this.kakaoPlaceId = kakaoPlaceId;
        this.name = name;
        this.address = address;
        this.roadAddress = roadAddress;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
        this.categoryGroupCode = categoryGroupCode;
        this.thumbnailUrl = thumbnailUrl;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Place create(
            String kakaoPlaceId,
            String name,
            String address,
            String roadAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            String category,
            String categoryGroupCode,
            String thumbnailUrl,
            Instant now
    ) {
        return new Place(
                kakaoPlaceId,
                name,
                address,
                roadAddress,
                latitude,
                longitude,
                category,
                categoryGroupCode,
                thumbnailUrl,
                now
        );
    }

    public Long getId() {
        return id;
    }

    public String getKakaoPlaceId() {
        return kakaoPlaceId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getRoadAddress() {
        return roadAddress;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getCategory() {
        return category;
    }

    public String getCategoryGroupCode() {
        return categoryGroupCode;
    }

    public String getCategoryName() {
        return DulpickPlaceCategory.fromKakao(categoryGroupCode, category).getDisplayName();
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }
}
