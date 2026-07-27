package kr.omong.dulpick.global.security;

import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.member.domain.MemberRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class MemberAccessTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_MEMBER = new OAuth2Error(
            "invalid_token",
            "Member access is no longer valid",
            null
    );

    private final MemberRepository memberRepository;

    public MemberAccessTokenValidator(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            Long memberId = Long.valueOf(jwt.getSubject());
            Number tokenVersion = jwt.getClaim("tokenVersion");
            boolean valid = tokenVersion != null
                    && memberRepository.findById(memberId)
                    .filter(Member::isActive)
                    .filter(member -> member.getTokenVersion() == tokenVersion.longValue())
                    .isPresent();
            if (valid) {
                return OAuth2TokenValidatorResult.success();
            }
        } catch (IllegalArgumentException exception) {
            return OAuth2TokenValidatorResult.failure(INVALID_MEMBER);
        }
        return OAuth2TokenValidatorResult.failure(INVALID_MEMBER);
    }
}
