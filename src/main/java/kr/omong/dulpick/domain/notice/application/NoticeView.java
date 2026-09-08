package kr.omong.dulpick.domain.notice.application;

import kr.omong.dulpick.domain.notice.domain.Notice;

import java.time.Instant;

public record NoticeView(
        Long noticeId,
        String title,
        String content,
        Instant createdAt,
        Instant updatedAt
) {

    public static NoticeView from(Notice notice) {
        return new NoticeView(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }
}
