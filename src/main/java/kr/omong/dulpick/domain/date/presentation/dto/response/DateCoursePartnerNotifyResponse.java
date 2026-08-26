package kr.omong.dulpick.domain.date.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.date.application.command.DateCoursePartnerNotified;

public record DateCoursePartnerNotifyResponse(
        @Schema(description = "상대방 알림 생성 요청 성공 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean notified,
        @Schema(description = "알림을 받은 커플 상대방 회원 ID", example = "124", requiredMode = Schema.RequiredMode.REQUIRED)
        Long partnerMemberId
) {

    public static DateCoursePartnerNotifyResponse from(DateCoursePartnerNotified result) {
        return new DateCoursePartnerNotifyResponse(result.notified(), result.partnerMemberId());
    }
}
