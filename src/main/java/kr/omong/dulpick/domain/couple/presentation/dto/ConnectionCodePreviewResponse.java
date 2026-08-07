package kr.omong.dulpick.domain.couple.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.couple.application.query.view.ConnectionCodePreview;

public record ConnectionCodePreviewResponse(
        @Schema(description = "연결 확정 전에 표시할 상대방 닉네임", example = "상대방")
        String nickname,
        @Schema(description = "iOS가 내장 에셋에 매핑할 상대방 프로필 아이콘 번호", example = "3", minimum = "1", maximum = "5")
        int profileIcon
) {

    public static ConnectionCodePreviewResponse from(ConnectionCodePreview preview) {
        return new ConnectionCodePreviewResponse(preview.nickname(), preview.profileIcon());
    }
}
