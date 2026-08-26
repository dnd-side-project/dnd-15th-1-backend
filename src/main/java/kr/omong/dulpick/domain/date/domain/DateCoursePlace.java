package kr.omong.dulpick.domain.date.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import kr.omong.dulpick.domain.date.domain.exception.InvalidDateCourseException;
import kr.omong.dulpick.domain.place.domain.Place;

import java.time.Instant;

@Entity
@Table(name = "date_course_places")
public class DateCoursePlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_course_id", nullable = false)
    private Long dateCourseId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DateCoursePlace() {
    }

    private DateCoursePlace(
            Long dateCourseId,
            Place place,
            int sequenceOrder,
            Instant createdAt
    ) {
        if (dateCourseId == null || dateCourseId <= 0) {
            throw new InvalidDateCourseException(
                    "dateCourseId",
                    "REQUIRED",
                    "데이트 코스 식별자가 필요합니다"
            );
        }
        if (place == null) {
            throw new InvalidDateCourseException(
                    "placeId",
                    "REQUIRED",
                    "장소 정보가 필요합니다"
            );
        }
        if (sequenceOrder <= 0) {
            throw new InvalidDateCourseException(
                    "sequenceOrder",
                    "OUT_OF_RANGE",
                    "장소 순서는 1 이상이어야 합니다"
            );
        }
        if (createdAt == null) {
            throw new InvalidDateCourseException(
                    "createdAt",
                    "REQUIRED",
                    "생성 시각이 필요합니다"
            );
        }
        this.dateCourseId = dateCourseId;
        this.place = place;
        this.sequenceOrder = sequenceOrder;
        this.createdAt = createdAt;
    }

    public static DateCoursePlace create(
            Long dateCourseId,
            Place place,
            int sequenceOrder,
            Instant createdAt
    ) {
        return new DateCoursePlace(dateCourseId, place, sequenceOrder, createdAt);
    }

    public Long getDateCourseId() {
        return dateCourseId;
    }

    public Place getPlace() {
        return place;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }
}
