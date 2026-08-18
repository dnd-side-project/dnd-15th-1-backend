package kr.omong.dulpick.domain.place.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "place_classifications")
public class PlaceClassification {

    @Id
    @Column(name = "place_id")
    private Long placeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment_type", length = 20)
    private PlaceEnvironment environment;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment_source", length = 20)
    private ClassificationSource environmentSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", length = 20)
    private PlaceActivity activity;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_source", length = 20)
    private ClassificationSource activitySource;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_type", length = 20)
    private PlaceTime time;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_source", length = 20)
    private ClassificationSource timeSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "focus_type", length = 20)
    private PlaceFocus focus;

    @Enumerated(EnumType.STRING)
    @Column(name = "focus_source", length = 20)
    private ClassificationSource focusSource;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PlaceClassification() {
    }

    private PlaceClassification(Long placeId, Instant now) {
        this.placeId = Objects.requireNonNull(placeId);
        this.createdAt = Objects.requireNonNull(now);
        this.updatedAt = now;
    }

    public static PlaceClassification initialize(Long placeId, Instant now) {
        return new PlaceClassification(placeId, now);
    }

    public void classifyEnvironment(
            PlaceEnvironment value,
            ClassificationSource source,
            Instant now
    ) {
        if (isProtectedManualValue(environmentSource, source)) {
            return;
        }
        this.environment = Objects.requireNonNull(value);
        this.environmentSource = Objects.requireNonNull(source);
        this.updatedAt = Objects.requireNonNull(now);
    }

    public void classifyActivity(
            PlaceActivity value,
            ClassificationSource source,
            Instant now
    ) {
        if (isProtectedManualValue(activitySource, source)) {
            return;
        }
        this.activity = Objects.requireNonNull(value);
        this.activitySource = Objects.requireNonNull(source);
        this.updatedAt = Objects.requireNonNull(now);
    }

    public void classifyTime(
            PlaceTime value,
            ClassificationSource source,
            Instant now
    ) {
        if (isProtectedManualValue(timeSource, source)) {
            return;
        }
        this.time = Objects.requireNonNull(value);
        this.timeSource = Objects.requireNonNull(source);
        this.updatedAt = Objects.requireNonNull(now);
    }

    public void classifyFocus(
            PlaceFocus value,
            ClassificationSource source,
            Instant now
    ) {
        if (isProtectedManualValue(focusSource, source)) {
            return;
        }
        this.focus = Objects.requireNonNull(value);
        this.focusSource = Objects.requireNonNull(source);
        this.updatedAt = Objects.requireNonNull(now);
    }

    public void clearEnvironment(Instant now) {
        this.environment = null;
        this.environmentSource = null;
        this.updatedAt = Objects.requireNonNull(now);
    }

    public void clearActivity(Instant now) {
        this.activity = null;
        this.activitySource = null;
        this.updatedAt = Objects.requireNonNull(now);
    }

    public void clearTime(Instant now) {
        this.time = null;
        this.timeSource = null;
        this.updatedAt = Objects.requireNonNull(now);
    }

    public void clearFocus(Instant now) {
        this.focus = null;
        this.focusSource = null;
        this.updatedAt = Objects.requireNonNull(now);
    }

    public PlaceClassificationStatus getStatus() {
        int classifiedCount = 0;
        classifiedCount += environment == null ? 0 : 1;
        classifiedCount += activity == null ? 0 : 1;
        classifiedCount += time == null ? 0 : 1;
        classifiedCount += focus == null ? 0 : 1;
        if (classifiedCount == 0) {
            return PlaceClassificationStatus.UNCLASSIFIED;
        }
        return classifiedCount == 4
                ? PlaceClassificationStatus.CLASSIFIED
                : PlaceClassificationStatus.PARTIALLY_CLASSIFIED;
    }

    private boolean isProtectedManualValue(
            ClassificationSource currentSource,
            ClassificationSource requestedSource
    ) {
        return currentSource == ClassificationSource.MANUAL
                && requestedSource == ClassificationSource.AI;
    }

    public Long getPlaceId() {
        return placeId;
    }

    public PlaceEnvironment getEnvironment() {
        return environment;
    }

    public ClassificationSource getEnvironmentSource() {
        return environmentSource;
    }

    public PlaceActivity getActivity() {
        return activity;
    }

    public ClassificationSource getActivitySource() {
        return activitySource;
    }

    public PlaceTime getTime() {
        return time;
    }

    public ClassificationSource getTimeSource() {
        return timeSource;
    }

    public PlaceFocus getFocus() {
        return focus;
    }

    public ClassificationSource getFocusSource() {
        return focusSource;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
