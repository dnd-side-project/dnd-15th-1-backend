package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.application.PublicContentView;
import org.springframework.data.domain.Page;

import java.util.List;

public record PublicContentPageResponse(
        List<PublicContentResponse> contents,
        @Schema(description = "0부터 시작하는 현재 페이지")
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static PublicContentPageResponse from(Page<PublicContentView> page) {
        return new PublicContentPageResponse(
                page.getContent().stream().map(PublicContentResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
