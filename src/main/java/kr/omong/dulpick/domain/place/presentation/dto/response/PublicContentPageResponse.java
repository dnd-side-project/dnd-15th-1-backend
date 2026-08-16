package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.application.PublicContentView;
import org.springframework.data.domain.Page;

import java.util.List;

public record PublicContentPageResponse(
        @Schema(description = "현재 페이지의 공개 콘텐츠 목록입니다. 없으면 빈 배열입니다.", example = "[]", requiredMode = Schema.RequiredMode.REQUIRED)
        List<PublicContentResponse> contents,
        @Schema(description = "0부터 시작하는 현재 페이지 번호", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        int page,
        @Schema(description = "페이지당 요청한 콘텐츠 수", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
        int size,
        @Schema(description = "검색 조건에 맞는 전체 콘텐츠 수", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        long totalElements,
        @Schema(description = "검색 조건에 맞는 전체 페이지 수", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        int totalPages,
        @Schema(description = "다음 페이지가 있으면 true입니다.", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
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
