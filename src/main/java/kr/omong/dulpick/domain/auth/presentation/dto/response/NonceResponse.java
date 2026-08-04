package kr.omong.dulpick.domain.auth.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.auth.application.command.result.IssuedNonce;
import kr.omong.dulpick.global.time.ServiceTime;

import java.time.LocalDateTime;

public record NonceResponse(
        @Schema(
                description = "provider 인증 요청에 사용할 일회성 nonce 원문",
                example = "l7JcLxgJx7c0nS0wqgWQeQ"
        )
        String nonce,
        @Schema(
                description = "nonce 만료 시각. 발급 후 10분이며 대한민국 표준시(UTC+9, Asia/Seoul) 기준입니다.",
                example = "2026-07-31T16:10:00"
        )
        LocalDateTime expiresAt
) {

    public static NonceResponse from(IssuedNonce issuedNonce) {
        return new NonceResponse(
                issuedNonce.nonce(),
                ServiceTime.toLocalDateTime(issuedNonce.expiresAt())
        );
    }
}
