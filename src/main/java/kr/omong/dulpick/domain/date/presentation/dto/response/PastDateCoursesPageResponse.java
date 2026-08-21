package kr.omong.dulpick.domain.date.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.date.application.query.view.DateCourseSummaryView;
import org.springframework.data.domain.Page;

import java.util.List;

public record PastDateCoursesPageResponse(
        @Schema(description = "현재 페이지의 지난 데이트 목록입니다. 없으면 빈 배열입니다.", example = "[]", requiredMode = Schema.RequiredMode.REQUIRED)
        List<DateCourseSummaryResponse> dateCourses,
        @Schema(description = "완료된 지난 데이트 일정의 총 횟수입니다.", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
        long totalCount,
        @Schema(description = "0부터 시작하는 현재 페이지 번호", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        int page,
        @Schema(description = "페이지당 요청한 데이트 수", example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
        int size,
        @Schema(description = "전체 페이지 수", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        int totalPages,
        @Schema(description = "다음 페이지가 있으면 true입니다.", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasNext
) {

    public static PastDateCoursesPageResponse from(Page<DateCourseSummaryView> page) {
        return new PastDateCoursesPageResponse(
                page.getContent().stream().map(DateCourseSummaryResponse::from).toList(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
