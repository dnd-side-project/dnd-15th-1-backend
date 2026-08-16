package kr.omong.dulpick.domain.date.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.date.application.query.view.HomeOverviewView;

public record HomeOverviewResponse(
        @Schema(example = "true")
        boolean connected,
        @Schema(example = "둘픽이")
        String myNickname,
        @Schema(nullable = true, description = "연결된 상대방 닉네임. 미연결이면 null", example = "오몽이")
        String partnerNickname,
        @Schema(nullable = true, description = "가장 가까운 확정 데이트 일정. 없으면 null")
        DateCourseSummaryResponse currentDateCourse
) {

    public static HomeOverviewResponse from(HomeOverviewView view) {
        return new HomeOverviewResponse(
                view.connected(),
                view.myNickname(),
                view.partnerNickname(),
                view.currentDateCourse() == null
                        ? null
                        : DateCourseSummaryResponse.from(view.currentDateCourse())
        );
    }
}
