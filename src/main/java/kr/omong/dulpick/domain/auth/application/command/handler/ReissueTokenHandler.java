package kr.omong.dulpick.domain.auth.application.command.handler;

import kr.omong.dulpick.domain.auth.application.command.result.IssuedTokens;
import kr.omong.dulpick.domain.auth.application.exception.ExpiredRefreshTokenException;
import kr.omong.dulpick.domain.auth.application.exception.InvalidRefreshTokenException;
import kr.omong.dulpick.domain.auth.application.support.TokenService;
import kr.omong.dulpick.domain.auth.domain.RefreshToken;
import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.domain.member.domain.exception.MemberNotActiveException;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Component
public class ReissueTokenHandler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final Clock clock;

    public ReissueTokenHandler(
            RefreshTokenRepository refreshTokenRepository,
            TokenService tokenService,
            Clock clock
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public IssuedTokens handle(String rawRefreshToken) {
        RefreshToken currentToken = refreshTokenRepository
                .findForUpdateByTokenHash(Sha256.hex(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        if (currentToken.wasRotated()) {
            return replayOrReject(currentToken, rawRefreshToken);
        }
        validateRefreshToken(currentToken);
        return tokenService.issueRotated(currentToken, rawRefreshToken);
    }

    private void validateRefreshToken(RefreshToken refreshToken) {
        if (refreshToken.isRevoked()) {
            throw new InvalidRefreshTokenException();
        }
        if (refreshToken.isExpired(clock.instant())) {
            throw new ExpiredRefreshTokenException();
        }
        if (!refreshToken.getMember().isActive()) {
            throw new MemberNotActiveException();
        }
    }

    private IssuedTokens replayOrReject(RefreshToken refreshToken, String rawRefreshToken) {
        return tokenService.issueWithinReplayGrace(refreshToken, rawRefreshToken)
                .orElseThrow(() -> rejectRotatedTokenReplay(refreshToken));
    }

    private InvalidRefreshTokenException rejectRotatedTokenReplay(RefreshToken refreshToken) {
        refreshTokenRepository.revokeAllByMemberId(
                refreshToken.getMember().getId(),
                clock.instant()
        );
        return new InvalidRefreshTokenException();
    }
}
