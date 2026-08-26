package kr.omong.dulpick.domain.couple.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.couple.application.query.view.ConnectionCodePreview;

@Schema(description = "연결 확정 전에 표시하는 상대방의 공개 기본 프로필")
public record ConnectionCodePreviewResponse(
        @Schema(description = "연결 확정 전에 표시할 상대방 닉네임. 사용자 인식 문자 기준 1~6자입니다.", example = "오몽이", minLength = 1, maxLength = 6, requiredMode = Schema.RequiredMode.REQUIRED)
        String nickname,
        @Schema(description = "iOS 프로필 에셋 번호(1~5)", example = "3", minimum = "1", maximum = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        int profileIcon
) {

    public static ConnectionCodePreviewResponse from(ConnectionCodePreview preview) {
        return new ConnectionCodePreviewResponse(preview.nickname(), preview.profileIcon());
    }
}
