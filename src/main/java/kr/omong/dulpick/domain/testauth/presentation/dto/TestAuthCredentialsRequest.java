package kr.omong.dulpick.domain.testauth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TestAuthCredentialsRequest(
        @Schema(
                description = "인증2 전용 로그인 이메일. 소문자로 정규화하여 사용합니다.",
                example = "swagger-test@example.com"
        )
        @NotBlank
        @Email
        @Size(max = 320)
        String email,
        @Schema(
                description = "인증2 전용 비밀번호. 영문, 숫자, 특수문자를 포함한 ASCII 문자 8~72자를 사용합니다.",
                example = "test-password-1234"
        )
        @NotBlank
        @Pattern(regexp = "^[\\x20-\\x7E]{8,72}$")
        String password
) {
}
