package kr.omong.dulpick.domain.date.application.command;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateDateCourseCommand(
        String title,
        LocalDate date,
        LocalTime time
) {
}
