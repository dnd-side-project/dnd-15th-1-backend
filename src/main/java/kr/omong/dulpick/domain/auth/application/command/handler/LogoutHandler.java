package kr.omong.dulpick.domain.auth.application.command.handler;

import kr.omong.dulpick.domain.auth.domain.RefreshToken;
import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Component
public class LogoutHandler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    public LogoutHandler(RefreshTokenRepository refreshTokenRepository, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    @Transactional
    public void handle(String rawRefreshToken, Long memberId) {
        refreshTokenRepository
                .findForUpdateByTokenHash(Sha256.hex(rawRefreshToken))
                .filter(token -> token.getMember().getId().equals(memberId))
                .ifPresent(token -> token.revoke(clock.instant()));
    }
}
