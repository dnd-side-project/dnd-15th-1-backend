package kr.omong.dulpick.domain.couple.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ConnectionCodeRequest(
        @NotBlank
        @Schema(
                description = "상대방의 영문 6자리 연결 코드. 서버 발급값은 대문자이며 입력 시 앞뒤 공백 제거 및 대문자 정규화를 적용합니다.",
                example = "ABCDEF",
                minLength = 6,
                maxLength = 6,
                pattern = "^[A-Za-z]{6}$"
        )
        String connectionCode
) {
}
