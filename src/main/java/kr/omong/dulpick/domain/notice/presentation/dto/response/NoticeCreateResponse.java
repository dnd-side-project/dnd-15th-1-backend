package kr.omong.dulpick.domain.notice.presentation.dto.response;

import kr.omong.dulpick.domain.notice.application.NoticeCreateView;

public record NoticeCreateResponse(
        NoticeResponse notice,
        NoticeNotificationResponse notification
) {

    public static NoticeCreateResponse from(NoticeCreateView view) {
        return new NoticeCreateResponse(
                NoticeResponse.from(view.notice()),
                view.notification() == null ? null : NoticeNotificationResponse.from(view.notification())
        );
    }
}
