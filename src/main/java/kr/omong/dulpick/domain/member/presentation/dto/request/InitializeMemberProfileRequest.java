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
                description = "iOS 프로필 에셋 번호(1~5). 최초 화면의 기본값은 1입니다.",
                minimum = "1",
                maximum = "5",
                example = "1"
        )
        Integer profileIcon,
        @Valid
        @Schema(
                description = "선택 입력입니다. 생략·null·네 필드 전체 빈 값이면 아직 설정하지 않은 상태로 저장됩니다.",
                nullable = true
        )
        InitializeDatePreferencesRequest datePreferences
) {

    public InitializeMemberProfileCommand toCommand() {
        return new InitializeMemberProfileCommand(
                nickname,
                profileIcon,
                datePreferences == null ? null : datePreferences.toDomainOrNull()
        );
    }
}
