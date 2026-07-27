package kr.omong.dulpick.domain.member.application;

public class MemberNotActiveException extends RuntimeException {

    public MemberNotActiveException() {
        super("Member is not active");
    }
}
