package kr.omong.dulpick.domain.date.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import kr.omong.dulpick.domain.date.domain.exception.InvalidDateCourseException;

import java.time.Instant;

@Entity
@Table(name = "date_courses")
public class DateCourse {

    private static final int MAX_TITLE_LENGTH = 120;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "couple_id", nullable = false)
    private Long coupleId;

    @Column(name = "created_by_member_id", nullable = false)
    private Long createdByMemberId;

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DateCourseStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DateCourse() {
    }

    private DateCourse(
            Long coupleId,
            Long createdByMemberId,
            String title,
            Instant scheduledAt,
            Instant createdAt
    ) {
        this.coupleId = requireCoupleId(coupleId);
        this.createdByMemberId = requireCreatorId(createdByMemberId);
        this.title = normalizeTitle(title);
        this.scheduledAt = requireScheduledAt(scheduledAt);
        this.status = DateCourseStatus.DRAFT;
        this.createdAt = requireCreatedAt(createdAt);
        this.updatedAt = createdAt;
    }

    public static DateCourse create(
            Long coupleId,
            Long createdByMemberId,
            String title,
            Instant scheduledAt,
            Instant createdAt
    ) {
        return new DateCourse(coupleId, createdByMemberId, title, scheduledAt, createdAt);
    }

    public void confirm(
            String title,
            Instant scheduledAt,
            Instant updatedAt
    ) {
        this.title = normalizeTitle(title);
        this.scheduledAt = requireScheduledAt(scheduledAt);
        this.status = DateCourseStatus.CONFIRMED;
        this.updatedAt = requireUpdatedAt(updatedAt);
    }

    public Long getId() {
        return id;
    }

    public Long getCoupleId() {
        return coupleId;
    }

    public String getTitle() {
        return title;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public DateCourseStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    private Long requireCoupleId(Long coupleId) {
        if (coupleId == null || coupleId <= 0) {
            throw new InvalidDateCourseException(
                    "coupleId",
                    "REQUIRED",
                    "커플 식별자가 필요합니다"
            );
        }
        return coupleId;
    }

    private Long requireCreatorId(Long createdByMemberId) {
        if (createdByMemberId == null || createdByMemberId <= 0) {
            throw new InvalidDateCourseException(
                    "createdByMemberId",
                    "REQUIRED",
                    "생성자 회원 식별자가 필요합니다"
            );
        }
        return createdByMemberId;
    }

    private String normalizeTitle(String title) {
        if (title == null) {
            throw new InvalidDateCourseException(
                    "title",
                    "REQUIRED",
                    "데이트명은 필수입니다"
            );
        }
        String normalized = title.strip();
        if (normalized.isBlank() || normalized.length() > MAX_TITLE_LENGTH) {
            throw new InvalidDateCourseException(
                    "title",
                    "OUT_OF_RANGE",
                    "데이트명은 1자 이상 120자 이하여야 합니다"
            );
        }
        return normalized;
    }

    private Instant requireScheduledAt(Instant scheduledAt) {
        if (scheduledAt == null) {
            throw new InvalidDateCourseException(
                    "scheduledAt",
                    "REQUIRED",
                    "데이트 일정 시각이 필요합니다"
            );
        }
        return scheduledAt;
    }

    private Instant requireCreatedAt(Instant createdAt) {
        if (createdAt == null) {
            throw new InvalidDateCourseException(
                    "createdAt",
                    "REQUIRED",
                    "생성 시각이 필요합니다"
            );
        }
        return createdAt;
    }

    private Instant requireUpdatedAt(Instant updatedAt) {
        if (updatedAt == null) {
            throw new InvalidDateCourseException(
                    "updatedAt",
                    "REQUIRED",
                    "수정 시각이 필요합니다"
            );
        }
        return updatedAt;
    }
}
