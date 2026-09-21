package kr.omong.dulpick.domain.notice.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.omong.dulpick.domain.notice.application.NoticeUpdateCommand;

import java.time.Instant;

public record UpdateNoticeRequest(
        @NotBlank
        @Size(max = 200)
        @Schema(description = "공지사항 제목", example = "둘픽 업데이트 안내")
        String title,
        @NotBlank
        @Size(max = 10_000)
        @Schema(description = "공지사항 내용. HTML이 아닌 일반 텍스트로 저장됩니다.", example = "수정된 공지사항 내용입니다.")
        String content,
        @NotNull
        @Schema(description = "조회 당시 공지사항의 updatedAt. 동시 수정 시 최신 내용으로 다시 조회하도록 충돌을 감지합니다.", example = "2026-09-07T01:00:00Z")
        Instant expectedUpdatedAt
) {

    public NoticeUpdateCommand toCommand() {
        return new NoticeUpdateCommand(title.strip(), content.strip(), expectedUpdatedAt);
    }
}
