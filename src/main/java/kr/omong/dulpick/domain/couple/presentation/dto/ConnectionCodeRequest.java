package kr.omong.dulpick.domain.couple.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeFormat;

public record ConnectionCodeRequest(
        @NotBlank
        @Schema(
                description = "상대방의 영문 5자리 연결 코드. 서버 발급값은 대문자이며 입력 시 앞뒤 공백 제거 및 대문자 정규화를 적용합니다.",
                example = "ABCDE",
                minLength = ConnectionCodeFormat.LENGTH,
                maxLength = ConnectionCodeFormat.LENGTH,
                pattern = ConnectionCodeFormat.INPUT_PATTERN
        )
        String connectionCode
) {
}
