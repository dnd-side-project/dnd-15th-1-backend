package kr.omong.dulpick.domain.couple.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeFormat;

@Schema(description = "커플 연결 요청. 상대방의 영문 대문자 5자리 연결 코드를 입력합니다.")
public record ConnectionCodeRequest(
        @NotBlank
        @Schema(
                description = "필수 입력. 상대방에게서 받은 영문 대문자 5자리 연결 코드입니다. 입력 시 앞뒤 공백을 제거하고 대문자로 정규화합니다.",
                example = "ABCDE",
                minLength = ConnectionCodeFormat.LENGTH,
                maxLength = ConnectionCodeFormat.LENGTH,
                pattern = ConnectionCodeFormat.INPUT_PATTERN
        )
        String connectionCode
) {
}
