package kr.omong.dulpick.domain.date.application.query.view;

public record HomeOverviewView(
        boolean connected,
        String myNickname,
        String partnerNickname,
        DateCourseSummaryView currentDateCourse
) {
}
