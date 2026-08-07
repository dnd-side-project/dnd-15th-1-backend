package kr.omong.dulpick.domain.couple.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.couple.application.support.IssuedConnectionCode;
import kr.omong.dulpick.domain.couple.domain.ConnectionCodeFormat;

public record ConnectionCodeResponse(
        @Schema(
                description = "상대방에게 전달할 현재 활성 영문 대문자 5자리 연결 코드",
                pattern = ConnectionCodeFormat.NORMALIZED_PATTERN,
                minLength = ConnectionCodeFormat.LENGTH,
                maxLength = ConnectionCodeFormat.LENGTH,
                example = "ABCDE"
        )
        String code,
        @Schema(description = "iOS 공유 및 딥링크 진입에 사용할 연결 URL")
        String shareUrl
) {

    public static ConnectionCodeResponse from(IssuedConnectionCode code) {
        return new ConnectionCodeResponse(code.code(), code.shareUrl());
    }
}
