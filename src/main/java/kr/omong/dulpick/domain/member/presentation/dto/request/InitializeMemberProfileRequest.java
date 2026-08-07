package kr.omong.dulpick.domain.member.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.omong.dulpick.domain.member.application.command.InitializeMemberProfileCommand;

public record InitializeMemberProfileRequest(
        @NotBlank
        @Schema(
                description = "닉네임. 앞뒤 공백을 제외한 사용자 인식 문자 기준 1~6자",
                minLength = 1,
                maxLength = 6,
                example = "둘픽이"
        )
        String nickname,
        @NotNull
        @Min(1)
        @Max(5)
        @Schema(
                description = "iOS 프로필 에셋 번호(1~5)",
                minimum = "1",
                maximum = "5",
                example = "1"
        )
        Integer profileIcon,
        @NotNull
        @Valid
        @Schema(description = "온보딩 완료를 위해 반드시 모두 선택해야 하는 4가지 데이트 성향")
        DatePreferencesRequest datePreferences
) {

    public InitializeMemberProfileCommand toCommand() {
        return new InitializeMemberProfileCommand(
                nickname,
                profileIcon,
                datePreferences.toDomain()
        );
    }
}
