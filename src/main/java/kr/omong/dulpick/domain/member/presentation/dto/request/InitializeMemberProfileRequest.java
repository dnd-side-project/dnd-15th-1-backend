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
                description = "최초 닉네임. 앞뒤 공백 제거 후 사용자 인식 문자 기준 1~6자이며, 공백만 또는 제어 문자는 허용하지 않습니다.",
                minLength = 1,
                maxLength = 6,
                example = "둘픽이"
        )
        String nickname,
        @NotNull
        @Min(1)
        @Max(5)
        @Schema(
                description = "프로필 아이콘 번호(1~5). iOS가 번호에 맞는 내장 에셋을 표시하며 서버는 이미지 URL을 제공하지 않습니다.",
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
