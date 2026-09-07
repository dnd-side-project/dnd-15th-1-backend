package kr.omong.dulpick.domain.notice.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.notice.application.NoticePageView;

import java.util.List;

public record NoticePageResponse(
        List<NoticeResponse> notices,
        @Schema(example = "0") int page,
        @Schema(example = "20") int size,
        @Schema(example = "12") long totalElements,
        @Schema(example = "1") int totalPages,
        @Schema(example = "false") boolean hasNext
) {

    public static NoticePageResponse from(NoticePageView view) {
        return new NoticePageResponse(
                view.notices().stream().map(NoticeResponse::from).toList(),
                view.page(),
                view.size(),
                view.totalElements(),
                view.totalPages(),
                view.hasNext()
        );
    }
}
