package kr.omong.dulpick.domain.member.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kr.omong.dulpick.domain.member.application.command.UpdateMemberProfileCommand;

public record UpdateMemberProfileRequest(
        @Schema(
                description = "변경할 닉네임. 생략하면 유지됩니다. 앞뒤 공백 제거 후 사용자 인식 문자 기준 1~6자입니다.",
                minLength = 1,
                maxLength = 6,
                example = "둘픽이"
        )
        String nickname,
        @Min(1)
        @Max(5)
        @Schema(
                description = "변경할 프로필 아이콘 번호(1~5). 생략하면 유지되며 iOS 내장 에셋과 매핑됩니다.",
                minimum = "1",
                maximum = "5",
                example = "3"
        )
        Integer profileIcon
) {

    public UpdateMemberProfileCommand toCommand() {
        return new UpdateMemberProfileCommand(nickname, profileIcon);
    }
}
