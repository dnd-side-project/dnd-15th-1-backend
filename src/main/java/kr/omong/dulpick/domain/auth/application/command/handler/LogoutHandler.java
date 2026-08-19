package kr.omong.dulpick.domain.auth.application.command.handler;

import kr.omong.dulpick.domain.auth.domain.RefreshToken;
import kr.omong.dulpick.domain.auth.domain.RefreshTokenRepository;
import kr.omong.dulpick.global.security.crypto.Sha256;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

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
        Instant loggedOutAt = clock.instant();
        refreshTokenRepository
                .findForUpdateByTokenHash(Sha256.hex(rawRefreshToken))
                .filter(token -> token.getMember().getId().equals(memberId))
                .ifPresent(token -> revokeTokenChain(token, memberId, loggedOutAt));
    }

    private void revokeTokenChain(
            RefreshToken token,
            Long memberId,
            Instant loggedOutAt
    ) {
        Set<String> visitedReplacementHashes = new HashSet<>();
        RefreshToken currentToken = token;
        while (currentToken != null) {
            currentToken.revoke(loggedOutAt);
            String replacementHash = currentToken.getReplacedByTokenHash();
            if (replacementHash == null || !visitedReplacementHashes.add(replacementHash)) {
                return;
            }
            currentToken = findReplacement(replacementHash, memberId);
        }
    }

    private RefreshToken findReplacement(String tokenHash, Long memberId) {
        return refreshTokenRepository.findForUpdateByTokenHash(tokenHash)
                .filter(token -> token.getMember().getId().equals(memberId))
                .orElse(null);
    }
}
