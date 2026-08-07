package kr.omong.dulpick.domain.member.presentation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kr.omong.dulpick.domain.member.application.command.UpdateMemberProfileCommand;

public record UpdateMemberProfileRequest(
        String nickname,
        @Min(1) @Max(5) Integer profileIcon
) {

    public UpdateMemberProfileCommand toCommand() {
        return new UpdateMemberProfileCommand(nickname, profileIcon);
    }
}
