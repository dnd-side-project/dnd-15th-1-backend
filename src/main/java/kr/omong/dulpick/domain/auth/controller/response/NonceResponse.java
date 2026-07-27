package kr.omong.dulpick.domain.auth.controller.response;

import kr.omong.dulpick.domain.auth.application.IssuedNonce;

import java.time.Instant;

public record NonceResponse(
        String nonce,
        Instant expiresAt
) {

    public static NonceResponse from(IssuedNonce issuedNonce) {
        return new NonceResponse(issuedNonce.nonce(), issuedNonce.expiresAt());
    }
}
