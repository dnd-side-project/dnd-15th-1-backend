package kr.omong.dulpick.global.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "요청 필드 단위의 검증 오류")
public record FieldErrorResponse(
        @Schema(
                description = "검증에 실패한 요청 필드명",
                example = "nickname",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String field,
        @Schema(
                description = "필드 오류를 구분하는 내부 사유 코드",
                example = "INVALID_LENGTH",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String reason,
        @Schema(
                description = "해당 필드에 대한 사용자 안내 메시지",
                example = "닉네임은 1~6자로 입력해주세요",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String message
) {
}
