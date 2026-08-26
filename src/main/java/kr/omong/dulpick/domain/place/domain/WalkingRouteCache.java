package kr.omong.dulpick.domain.place.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "walking_route_cache")
public class WalkingRouteCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_place_id", nullable = false)
    private Long fromPlaceId;

    @Column(name = "to_place_id", nullable = false)
    private Long toPlaceId;

    @Column(name = "from_latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal fromLatitude;

    @Column(name = "from_longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal fromLongitude;

    @Column(name = "to_latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal toLatitude;

    @Column(name = "to_longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal toLongitude;

    @Column(name = "distance_meters", nullable = false)
    private int distanceMeters;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WalkingRouteCache() {
    }

    private WalkingRouteCache(
            Long fromPlaceId,
            Long toPlaceId,
            BigDecimal fromLatitude,
            BigDecimal fromLongitude,
            BigDecimal toLatitude,
            BigDecimal toLongitude,
            int distanceMeters,
            int durationSeconds,
            Instant now
    ) {
        this.fromPlaceId = Objects.requireNonNull(fromPlaceId);
        this.toPlaceId = Objects.requireNonNull(toPlaceId);
        this.fromLatitude = Objects.requireNonNull(fromLatitude);
        this.fromLongitude = Objects.requireNonNull(fromLongitude);
        this.toLatitude = Objects.requireNonNull(toLatitude);
        this.toLongitude = Objects.requireNonNull(toLongitude);
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.createdAt = Objects.requireNonNull(now);
        this.updatedAt = now;
    }

    public static WalkingRouteCache create(
            Long fromPlaceId,
            Long toPlaceId,
            BigDecimal fromLatitude,
            BigDecimal fromLongitude,
            BigDecimal toLatitude,
            BigDecimal toLongitude,
            int distanceMeters,
            int durationSeconds,
            Instant now
    ) {
        return new WalkingRouteCache(
                fromPlaceId,
                toPlaceId,
                fromLatitude,
                fromLongitude,
                toLatitude,
                toLongitude,
                distanceMeters,
                durationSeconds,
                now
        );
    }

    public boolean matchesCoordinates(
            BigDecimal fromLatitude,
            BigDecimal fromLongitude,
            BigDecimal toLatitude,
            BigDecimal toLongitude
    ) {
        return sameCoordinate(this.fromLatitude, fromLatitude)
                && sameCoordinate(this.fromLongitude, fromLongitude)
                && sameCoordinate(this.toLatitude, toLatitude)
                && sameCoordinate(this.toLongitude, toLongitude);
    }

    public void refresh(
            BigDecimal fromLatitude,
            BigDecimal fromLongitude,
            BigDecimal toLatitude,
            BigDecimal toLongitude,
            int distanceMeters,
            int durationSeconds,
            Instant now
    ) {
        this.fromLatitude = Objects.requireNonNull(fromLatitude);
        this.fromLongitude = Objects.requireNonNull(fromLongitude);
        this.toLatitude = Objects.requireNonNull(toLatitude);
        this.toLongitude = Objects.requireNonNull(toLongitude);
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.updatedAt = Objects.requireNonNull(now);
    }

    public Long getFromPlaceId() {
        return fromPlaceId;
    }

    public Long getToPlaceId() {
        return toPlaceId;
    }

    public int getDistanceMeters() {
        return distanceMeters;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    private static boolean sameCoordinate(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) == 0;
    }
}
