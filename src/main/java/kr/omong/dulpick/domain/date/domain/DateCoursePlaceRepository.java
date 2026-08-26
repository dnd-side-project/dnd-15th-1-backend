package kr.omong.dulpick.domain.date.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DateCoursePlaceRepository extends JpaRepository<DateCoursePlace, Long> {

    @EntityGraph(attributePaths = "place")
    List<DateCoursePlace> findAllByDateCourseIdOrderBySequenceOrderAsc(Long dateCourseId);

    void deleteAllByDateCourseId(Long dateCourseId);

    long countByDateCourseId(Long dateCourseId);
}
