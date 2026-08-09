package kr.omong.dulpick.domain.member.application.command;

public record UpdateMemberProfileCommand(
        String nickname,
        Integer profileIcon
) {
}
