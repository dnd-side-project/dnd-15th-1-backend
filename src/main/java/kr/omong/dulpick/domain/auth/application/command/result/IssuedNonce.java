package kr.omong.dulpick.domain.auth.application.command.result;

import java.time.Instant;

public record IssuedNonce(
        String nonce,
        Instant expiresAt
) {
}
