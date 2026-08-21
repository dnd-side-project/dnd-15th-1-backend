package kr.omong.dulpick.domain.date.application.command;

public record DateCoursePartnerNotified(
        boolean notified,
        Long partnerMemberId
) {
}
