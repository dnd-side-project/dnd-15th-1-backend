package kr.omong.dulpick.domain.member.application.query.reader;

import kr.omong.dulpick.domain.auth.domain.SocialAccount;
import kr.omong.dulpick.domain.auth.domain.SocialAccountRepository;
import kr.omong.dulpick.domain.member.application.exception.MemberNotFoundException;
import kr.omong.dulpick.domain.member.application.query.view.MemberProfile;
import kr.omong.dulpick.domain.member.application.query.view.MemberSocialAccount;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MemberProfileReader {

    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;

    public MemberProfileReader(
            MemberRepository memberRepository,
            SocialAccountRepository socialAccountRepository
    ) {
        this.memberRepository = memberRepository;
        this.socialAccountRepository = socialAccountRepository;
    }

    public MemberProfile read(Long memberId) {
        Member member = findActiveMember(memberId);
        List<MemberSocialAccount> accounts = socialAccountRepository
                .findAllByMemberId(memberId)
                .stream()
                .map(this::toSocialAccount)
                .toList();
        return toProfile(member, accounts);
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

    private MemberProfile toProfile(
            Member member,
            List<MemberSocialAccount> accounts
    ) {
        return new MemberProfile(
                member.getId(),
                member.getStatus(),
                member.getCreatedAt(),
                member.getUpdatedAt(),
                member.getLastWithdrawnAt(),
                member.getLastRejoinedAt(),
                accounts
        );
    }
}
