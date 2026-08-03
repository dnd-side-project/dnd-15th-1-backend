package kr.omong.dulpick.domain.testauth.application;

import kr.omong.dulpick.domain.auth.application.command.AuthCommandService;
import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.exception.InvalidRefreshTokenException;
import kr.omong.dulpick.domain.auth.application.support.SocialAccountService;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.auth.application.support.model.AuthenticatedMember;
import kr.omong.dulpick.domain.auth.application.support.model.ProviderAuthorization;
import kr.omong.dulpick.domain.auth.domain.RefreshToken;
import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.domain.auth.domain.SocialProvider;
import kr.omong.dulpick.domain.member.domain.Member;
import kr.omong.dulpick.domain.testauth.domain.TestAuthCredential;
import kr.omong.dulpick.domain.testauth.domain.TestAuthCredentialRepository;
import kr.omong.dulpick.global.security.Sha256;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;

@Service
@ConditionalOnProperty(
        prefix = "features.test-auth",
        name = "enabled",
        havingValue = "true"
)
public class TestAuthService {

    private static final String PROVIDER_SUBJECT_PREFIX = "test-auth:";

    private final TestAuthCredentialRepository credentialRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SocialAccountService socialAccountService;
    private final TokenService tokenService;
    private final AuthCommandService authCommandService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public TestAuthService(
            TestAuthCredentialRepository credentialRepository,
            RefreshTokenRepository refreshTokenRepository,
            SocialAccountService socialAccountService,
            TokenService tokenService,
            AuthCommandService authCommandService,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.credentialRepository = credentialRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.socialAccountService = socialAccountService;
        this.tokenService = tokenService;
        this.authCommandService = authCommandService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public TestAuthResult signUp(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        rejectDuplicateEmail(normalizedEmail);

        try {
            AuthenticatedMember authenticatedMember = socialAccountService.getOrCreate(
                    SocialProvider.KAKAO,
                    providerSubject(normalizedEmail),
                    normalizedEmail,
                    ProviderAuthorization.none()
            );
            rejectExistingSocialAccount(authenticatedMember);
            saveCredential(authenticatedMember.member(), normalizedEmail, password);
            return issue(authenticatedMember.member());
        } catch (DataIntegrityViolationException exception) {
            throw new TestAuthEmailAlreadyExistsException(exception);
        }
    }

    @Transactional
    public TestAuthResult login(String email, String password) {
        TestAuthCredential credential = credentialRepository
                .findByEmail(normalizeEmail(email))
                .filter(saved -> passwordEncoder.matches(password, saved.getPasswordHash()))
                .orElseThrow(TestAuthAuthenticationException::new);
        Member member = credential.getMember();
        if (!member.isActive()) {
            member.rejoin(clock.instant());
        }
        return issue(member);
    }

    @Transactional
    public IssuedTokens reissue(String refreshToken) {
        validateTestAuthRefreshToken(refreshToken);
        return authCommandService.reissue(refreshToken);
    }

    @Transactional
    public void logout(String refreshToken, Long memberId) {
        if (!credentialRepository.existsByMemberId(memberId)) {
            throw new TestAuthAuthenticationException();
        }
        authCommandService.logout(refreshToken, memberId);
    }

    private void rejectDuplicateEmail(String email) {
        if (credentialRepository.existsByEmail(email)) {
            throw new TestAuthEmailAlreadyExistsException();
        }
    }

    private void rejectExistingSocialAccount(AuthenticatedMember authenticatedMember) {
        if (!authenticatedMember.newMember()) {
            throw new TestAuthEmailAlreadyExistsException();
        }
    }

    private void saveCredential(Member member, String email, String password) {
        TestAuthCredential credential = TestAuthCredential.create(
                member,
                email,
                passwordEncoder.encode(password),
                clock.instant()
        );
        credentialRepository.saveAndFlush(credential);
    }

    private TestAuthResult issue(Member member) {
        return new TestAuthResult(member.getId(), tokenService.issue(member));
    }

    private void validateTestAuthRefreshToken(String rawRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(Sha256.hex(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        if (!credentialRepository.existsByMemberId(refreshToken.getMember().getId())) {
            throw new InvalidRefreshTokenException();
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String providerSubject(String email) {
        return PROVIDER_SUBJECT_PREFIX + Sha256.hex(email);
    }
}
