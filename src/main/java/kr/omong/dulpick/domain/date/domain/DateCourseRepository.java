package kr.omong.dulpick.domain.date.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DateCourseRepository extends JpaRepository<DateCourse, Long> {

    Optional<DateCourse> findByIdAndCoupleId(Long id, Long coupleId);

    @Query("""
            SELECT dateCourse
            FROM DateCourse dateCourse
            WHERE dateCourse.coupleId = :coupleId
              AND dateCourse.status = :status
              AND dateCourse.scheduledAt >= :scheduledAfterOrAt
            ORDER BY dateCourse.scheduledAt ASC, dateCourse.id ASC
            """)
    List<DateCourse> findUpcoming(
            @Param("coupleId") Long coupleId,
            @Param("status") DateCourseStatus status,
            @Param("scheduledAfterOrAt") Instant scheduledAfterOrAt,
            Pageable pageable
    );

    @Query("""
            SELECT dateCourse
            FROM DateCourse dateCourse
            WHERE dateCourse.coupleId = :coupleId
              AND dateCourse.status = :status
              AND dateCourse.scheduledAt < :scheduledBefore
            ORDER BY dateCourse.scheduledAt DESC, dateCourse.id DESC
            """)
    List<DateCourse> findPast(
            @Param("coupleId") Long coupleId,
            @Param("status") DateCourseStatus status,
            @Param("scheduledBefore") Instant scheduledBefore,
            Pageable pageable
    );
}
