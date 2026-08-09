package kr.omong.dulpick.global.exception;

public record FieldErrorResponse(
        String field,
        String reason,
        String message
) {
}
