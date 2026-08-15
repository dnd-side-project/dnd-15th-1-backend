package kr.omong.dulpick.domain.member.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kr.omong.dulpick.domain.member.application.command.UpdateMemberProfileCommand;

@Schema(description = "기본 프로필 부분 수정 요청. nickname과 profileIcon 중 하나 이상을 입력해야 합니다.")
public record UpdateMemberProfileRequest(
        @Schema(
                description = "선택 입력. 앞뒤 공백을 제외한 사용자 인식 문자 기준 1~6자이며, 생략하면 기존 값을 유지합니다.",
                minLength = 1,
                maxLength = 6,
                example = "둘픽이",
                nullable = true
        )
        String nickname,
        @Min(1)
        @Max(5)
        @Schema(
                description = "선택 입력. iOS 프로필 에셋 번호(1~5)이며, 생략하면 기존 값을 유지합니다.",
                minimum = "1",
                maximum = "5",
                example = "3",
                nullable = true
        )
        Integer profileIcon
) {

    public UpdateMemberProfileCommand toCommand() {
        return new UpdateMemberProfileCommand(nickname, profileIcon);
    }
}
