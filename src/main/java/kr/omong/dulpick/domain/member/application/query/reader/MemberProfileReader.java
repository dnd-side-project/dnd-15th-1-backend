package kr.omong.dulpick.domain.member.application.query.reader;

import kr.omong.dulpick.domain.auth.domain.SocialAccount;
import kr.omong.dulpick.domain.auth.domain.SocialAccountRepository;
import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.domain.member.application.query.view.MemberDatePreferences;
import kr.omong.dulpick.domain.member.application.query.view.MemberProfileView;
import kr.omong.dulpick.domain.member.application.query.view.MemberSocialAccount;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberProfile;
import kr.omong.dulpick.domain.member.domain.MemberProfileRepository;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MemberProfileReader {

    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final MemberProfileRepository memberProfileRepository;

    public MemberProfileReader(
            MemberRepository memberRepository,
            SocialAccountRepository socialAccountRepository,
            MemberProfileRepository memberProfileRepository
    ) {
        this.memberRepository = memberRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.memberProfileRepository = memberProfileRepository;
    }

    public MemberProfileView read(Long memberId) {
        Member member = findActiveMember(memberId);
        List<MemberSocialAccount> accounts = socialAccountRepository
                .findAllByMemberId(memberId)
                .stream()
                .map(this::toSocialAccount)
                .toList();
        MemberProfile profile = memberProfileRepository.findById(memberId).orElse(null);
        return toProfile(member, profile, accounts);
    }

    private Member findActiveMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        if (!member.isActive()) {
            throw new MemberNotActiveException();
        }
        return member;
    }

    private MemberSocialAccount toSocialAccount(SocialAccount account) {
        return new MemberSocialAccount(account.getProvider(), account.getEmail());
    }

    private MemberProfileView toProfile(
            Member member,
            MemberProfile profile,
            List<MemberSocialAccount> accounts
    ) {
        return new MemberProfileView(
                member.getId(),
                member.getStatus(),
                profile != null,
                profile == null ? null : profile.getNickname(),
                profile == null ? null : profile.getProfileIcon(),
                toDatePreferences(profile),
                member.getCreatedAt(),
                member.getUpdatedAt(),
                member.getLastWithdrawnAt(),
                member.getLastRejoinedAt(),
                accounts
        );
    }

    private MemberDatePreferences toDatePreferences(MemberProfile profile) {
        if (profile == null) {
            return null;
        }
        var preferences = profile.getDatePreferences();
        return new MemberDatePreferences(
                preferences.indoorOutdoor(),
                preferences.activityLevel(),
                preferences.dateTime(),
                preferences.dateFocus()
        );
    }
}
