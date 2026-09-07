package kr.omong.dulpick.domain.notice.application;

import org.springframework.data.domain.Page;

import java.util.List;

public record NoticePageView(
        List<NoticeView> notices,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static NoticePageView from(Page<NoticeView> page) {
        return new NoticePageView(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
