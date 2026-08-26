package kr.omong.dulpick.domain.place.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "dulpick_category_code", length = 30)
    private DulpickPlaceCategory dulpickCategoryCode;

    @Column(length = 50)
    private String phone;

    @Column(name = "kakao_place_url", length = 1_000)
    private String kakaoPlaceUrl;

    @Column(name = "thumbnail_url", length = 1_000)
    private String thumbnailUrl;

    @OneToMany
    @JoinColumn(name = "place_id", insertable = false, updatable = false)
    @OrderBy("displayOrder ASC")
    @BatchSize(size = 50)
    private List<PlaceImage> images = new ArrayList<>();

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
            String phone,
            String kakaoPlaceUrl,
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
        this.phone = phone;
        this.kakaoPlaceUrl = kakaoPlaceUrl;
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
        return create(
                kakaoPlaceId,
                name,
                address,
                roadAddress,
                latitude,
                longitude,
                category,
                categoryGroupCode,
                null,
                null,
                thumbnailUrl,
                now
        );
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
            String phone,
            String kakaoPlaceUrl,
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
                phone,
                kakaoPlaceUrl,
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

    public DulpickPlaceCategory getDulpickCategory() {
        return dulpickCategoryCode == null
                ? DulpickPlaceCategory.fromKakao(categoryGroupCode, category)
                : dulpickCategoryCode;
    }

    public DulpickPlaceCategory getDulpickCategory(
            String fallbackCategoryGroupCode,
            String fallbackCategory
    ) {
        if (dulpickCategoryCode != null) {
            return dulpickCategoryCode;
        }
        if (!DulpickPlaceCategory.isFallback(categoryGroupCode, category)) {
            return DulpickPlaceCategory.fromKakao(categoryGroupCode, category);
        }
        return DulpickPlaceCategory.fromKakao(fallbackCategoryGroupCode, fallbackCategory);
    }

    public void updateDetails(
            String name,
            String address,
            String roadAddress,
            String category,
            String categoryGroupCode,
            String phone,
            String kakaoPlaceUrl,
            Instant now
    ) {
        this.name = required(name, this.name);
        this.address = required(address, this.address);
        this.roadAddress = optional(roadAddress, this.roadAddress);
        this.category = optional(category, this.category);
        this.categoryGroupCode = optional(categoryGroupCode, this.categoryGroupCode);
        this.phone = optional(phone, this.phone);
        this.kakaoPlaceUrl = optional(kakaoPlaceUrl, this.kakaoPlaceUrl);
        this.updatedAt = now;
    }

    private String required(String requested, String current) {
        return requested == null || requested.isBlank() ? current : requested.strip();
    }

    private String optional(String requested, String current) {
        return requested == null ? current : requested.strip().isEmpty() ? null : requested.strip();
    }

    public String getPhone() {
        return phone;
    }

    public String getKakaoPlaceUrl() {
        return kakaoPlaceUrl;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCategoryName() {
        return getDulpickCategory().getDisplayName();
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public List<String> getImageUrls() {
        return images.stream()
                .map(PlaceImage::getImageUrl)
                .toList();
    }
}
