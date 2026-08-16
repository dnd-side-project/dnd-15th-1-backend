package kr.omong.dulpick.domain.couple.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.couple.application.query.view.CoupleConnectionStatus;
import kr.omong.dulpick.global.time.ServiceTime;

import java.time.LocalDateTime;

public record CoupleConnectionStatusResponse(
        @Schema(description = "현재 활성 커플 연결 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean connected,
        @Schema(description = "요청 회원의 최신 기본 프로필. 연결 여부와 관계없이 반환합니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        CoupleMemberProfileResponse me,
        @Schema(description = "연결 상대방의 최신 기본 프로필. 연결 상태에서는 nickname과 profileIcon을 포함하며, 미연결일 때만 null입니다.", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        CoupleMemberProfileResponse partner,
        @Schema(description = "커플 연결 시각. Asia/Seoul 기준이며 미연결이면 null입니다.", nullable = true, format = "date-time", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        LocalDateTime connectedAt,
        @Schema(description = "연결일을 1일째로 계산한 함께한 일수. 미연결이면 null입니다.", example = "1", nullable = true, minimum = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Long daysTogether
) {

    public static CoupleConnectionStatusResponse from(CoupleConnectionStatus status) {
        return new CoupleConnectionStatusResponse(
                status.connected(),
                CoupleMemberProfileResponse.from(status.me()),
                CoupleMemberProfileResponse.from(status.partner()),
                ServiceTime.toLocalDateTime(status.connectedAt()),
                status.daysTogether()
        );
    }
}
