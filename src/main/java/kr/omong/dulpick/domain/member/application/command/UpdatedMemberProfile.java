package kr.omong.dulpick.domain.member.application.command;

public record UpdatedMemberProfile(
        String nickname,
        int profileIcon
) {
}
