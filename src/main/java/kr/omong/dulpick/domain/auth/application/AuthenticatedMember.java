package kr.omong.dulpick.domain.auth.application;

import kr.omong.dulpick.domain.member.domain.Member;

public record AuthenticatedMember(
        Member member,
        boolean newMember
) {
}
