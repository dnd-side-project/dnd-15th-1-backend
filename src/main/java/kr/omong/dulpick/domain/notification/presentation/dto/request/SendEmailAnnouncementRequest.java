package kr.omong.dulpick.domain.notification.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "운영자 이메일 공지 발송 요청")
public record SendEmailAnnouncementRequest(
        @NotBlank @Size(max = 200) @Schema(example = "개인정보처리방침 변경 안내", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,
        @NotBlank @Size(max = 5000) @Schema(example = "안녕하세요, 둘픽입니다.\\n개인정보처리방침이 아래와 같이 변경되었습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        String body
) {
}
