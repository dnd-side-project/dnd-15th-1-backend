package kr.omong.dulpick.domain.couple.presentation.dto;

import kr.omong.dulpick.domain.couple.application.support.IssuedConnectionCode;

public record ConnectionCodeResponse(
        String code,
        String shareUrl
) {

    public static ConnectionCodeResponse from(IssuedConnectionCode code) {
        return new ConnectionCodeResponse(code.code(), code.shareUrl());
    }
}
