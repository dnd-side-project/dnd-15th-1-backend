package kr.omong.dulpick.global.exception;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@Schema(
        description = "모든 API 오류에 공통으로 사용하는 응답입니다. fieldErrors는 입력값 검증 오류에서만 포함됩니다.",
        example = """
                {
                  "code": "INVALID_INPUT",
                  "message": "입력값이 올바르지 않습니다",
                  "fieldErrors": [
                    {
                      "field": "nickname",
                      "reason": "INVALID_LENGTH",
                      "message": "닉네임은 1~6자로 입력해주세요"
                    }
                  ]
                }
                """
)
public record ErrorResponse(
        @Schema(
                description = "클라이언트가 오류 유형을 구분할 때 사용하는 오류 코드",
                example = "INVALID_INPUT",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String code,
        @Schema(
                description = "사용자에게 안내할 오류 메시지",
                example = "입력값이 올바르지 않습니다",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String message,
        @ArraySchema(
                schema = @Schema(implementation = FieldErrorResponse.class),
                arraySchema = @Schema(
                        description = "입력값 검증 오류 목록. 일반 비즈니스 오류에서는 생략됩니다.",
                        nullable = true,
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED
                )
        )
        @Schema(example = "[]")
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<FieldErrorResponse> fieldErrors
) {

    public ErrorResponse {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse from(
            ErrorCode errorCode,
            List<FieldErrorResponse> fieldErrors
    ) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), fieldErrors);
    }
}
