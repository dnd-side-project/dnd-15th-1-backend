package kr.omong.dulpick.domain.notice.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kr.omong.dulpick.domain.notice.application.NoticeCommand;

public record CreateNoticeRequest(
        @NotBlank
        @Size(max = 200)
        @Schema(description = "공지사항 제목", example = "둘픽 업데이트 안내")
        String title,
        @NotBlank
        @Size(max = 10_000)
        @Schema(description = "공지사항 내용. HTML이 아닌 일반 텍스트로 저장됩니다.", example = "새로운 기능이 추가되었습니다.")
        String content,
        @Schema(description = "true이면 전체 활성 회원의 알림함과 푸시 큐에 새 글 알림을 등록합니다.", example = "false")
        boolean sendNotification
) {

    public NoticeCommand toCommand() {
        return new NoticeCommand(title.strip(), content.strip(), sendNotification);
    }
}
