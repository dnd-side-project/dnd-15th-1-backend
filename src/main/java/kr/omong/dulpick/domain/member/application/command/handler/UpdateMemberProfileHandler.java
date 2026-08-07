package kr.omong.dulpick.domain.member.application.command.handler;

import kr.omong.dulpick.domain.member.application.command.UpdateMemberProfileCommand;
import kr.omong.dulpick.domain.member.application.command.UpdatedMemberProfile;
import kr.omong.dulpick.domain.member.application.exception.MemberProfileRequiredException;
import kr.omong.dulpick.domain.member.domain.MemberProfile;
import kr.omong.dulpick.domain.member.domain.MemberProfileRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Component
public class UpdateMemberProfileHandler {

    private final MemberProfileRepository memberProfileRepository;
    private final Clock clock;

    public UpdateMemberProfileHandler(
            MemberProfileRepository memberProfileRepository,
            Clock clock
    ) {
        this.memberProfileRepository = memberProfileRepository;
        this.clock = clock;
    }

    @Transactional
    public UpdatedMemberProfile handle(Long memberId, UpdateMemberProfileCommand command) {
        MemberProfile profile = memberProfileRepository.findById(memberId)
                .orElseThrow(MemberProfileRequiredException::new);
        profile.updateBasicProfile(
                command.nickname(),
                command.profileIcon(),
                clock.instant()
        );
        return new UpdatedMemberProfile(profile.getNickname(), profile.getProfileIcon());
    }
}
