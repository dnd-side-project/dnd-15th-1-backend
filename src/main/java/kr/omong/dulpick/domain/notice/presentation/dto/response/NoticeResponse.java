package kr.omong.dulpick.domain.notice.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notice.application.NoticeView;

import java.time.Instant;

public record NoticeResponse(
        @Schema(description = "공지사항 식별자", example = "42")
        Long noticeId,
        @Schema(description = "공지사항 제목", example = "둘픽 업데이트 안내")
        String title,
        @Schema(description = "공지사항 내용", example = "새로운 기능이 추가되었습니다.")
        String content,
        @Schema(description = "작성 시각", example = "2026-09-07T01:00:00Z")
        Instant createdAt,
        @Schema(description = "최근 수정 시각", example = "2026-09-07T01:00:00Z")
        Instant updatedAt
) {

    public static NoticeResponse from(NoticeView view) {
        return new NoticeResponse(
                view.noticeId(),
                view.title(),
                view.content(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
