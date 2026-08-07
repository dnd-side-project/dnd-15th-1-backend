package kr.omong.dulpick.domain.member.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.omong.dulpick.domain.member.application.command.InitializeMemberProfileCommand;

public record InitializeMemberProfileRequest(
        @NotBlank String nickname,
        @NotNull @Min(1) @Max(5) Integer profileIcon,
        @NotNull @Valid DatePreferencesRequest datePreferences
) {

    public InitializeMemberProfileCommand toCommand() {
        return new InitializeMemberProfileCommand(
                nickname,
                profileIcon,
                datePreferences.toDomain()
        );
    }
}
