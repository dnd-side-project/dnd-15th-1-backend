package kr.omong.dulpick.domain.member.application;

import kr.omong.dulpick.domain.auth.domain.SocialAccount;
import kr.omong.dulpick.domain.auth.domain.SocialAccountRepository;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberQueryService {

    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;

    public MemberQueryService(
            MemberRepository memberRepository,
            SocialAccountRepository socialAccountRepository
    ) {
        this.memberRepository = memberRepository;
        this.socialAccountRepository = socialAccountRepository;
    }

    @Transactional(readOnly = true)
    public MemberProfile getMyProfile(Long memberId) {
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
