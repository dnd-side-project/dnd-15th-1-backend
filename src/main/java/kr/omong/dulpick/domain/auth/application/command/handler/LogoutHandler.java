package kr.omong.dulpick.domain.auth.application.command.handler;

import kr.omong.dulpick.domain.auth.domain.RefreshToken;
import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LogoutHandler {

    private final RefreshTokenRepository refreshTokenRepository;

    public LogoutHandler(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public void handle(String rawRefreshToken, Long memberId) {
        refreshTokenRepository
                .findForUpdateByTokenHash(Sha256.hex(rawRefreshToken))
                .filter(token -> token.getMember().getId().equals(memberId))
                .ifPresent(RefreshToken::revoke);
    }
}
