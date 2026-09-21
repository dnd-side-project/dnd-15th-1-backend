package kr.omong.dulpick.domain.notice.application;

import java.time.Instant;

public record NoticeUpdateCommand(
        String title,
        String content,
        Instant expectedUpdatedAt
) {
}
