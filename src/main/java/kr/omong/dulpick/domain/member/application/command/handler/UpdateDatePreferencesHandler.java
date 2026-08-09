package kr.omong.dulpick.domain.member.application.command.handler;

import kr.omong.dulpick.domain.member.application.exception.MemberProfileRequiredException;
import kr.omong.dulpick.domain.member.domain.DatePreferences;
import kr.omong.dulpick.domain.member.domain.MemberProfile;
import kr.omong.dulpick.domain.member.domain.MemberProfileRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Component
public class UpdateDatePreferencesHandler {

    private final MemberProfileRepository memberProfileRepository;
    private final Clock clock;

    public UpdateDatePreferencesHandler(
            MemberProfileRepository memberProfileRepository,
            Clock clock
    ) {
        this.memberProfileRepository = memberProfileRepository;
        this.clock = clock;
    }

    @Transactional
    public DatePreferences handle(Long memberId, DatePreferences preferences) {
        MemberProfile profile = memberProfileRepository.findById(memberId)
                .orElseThrow(MemberProfileRequiredException::new);
        profile.updateDatePreferences(preferences, clock.instant());
        return profile.getDatePreferences();
    }
}
