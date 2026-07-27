package kr.omong.dulpick.domain.auth.application;

import java.time.Instant;

public record IssuedNonce(
        String nonce,
        Instant expiresAt
) {
}
