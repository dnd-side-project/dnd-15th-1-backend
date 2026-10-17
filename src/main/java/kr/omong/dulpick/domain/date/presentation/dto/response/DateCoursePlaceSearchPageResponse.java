package kr.omong.dulpick.domain.date.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.date.application.query.view.DateCoursePlaceSearchPageView;

import java.util.List;

public record DateCoursePlaceSearchPageResponse(
        @ArraySchema(schema = @Schema(implementation = DateCoursePlacePoolResponse.DateCoursePlaceCandidateResponse.class))
        @Schema(description = "검색 조건에 맞는 커플 저장 장소. 결과가 없으면 빈 배열입니다.", example = "[]")
        List<DateCoursePlacePoolResponse.DateCoursePlaceCandidateResponse> places,
        @Schema(description = "0부터 시작하는 현재 페이지 번호", example = "0")
        int page,
        @Schema(description = "페이지당 장소 수", example = "10")
        int size,
        @Schema(description = "다음 페이지가 있으면 true입니다.", example = "false")
        boolean hasNext
) {

    public static DateCoursePlaceSearchPageResponse from(
            DateCoursePlaceSearchPageView view
    ) {
        return new DateCoursePlaceSearchPageResponse(
                view.places().stream()
                        .map(DateCoursePlacePoolResponse.DateCoursePlaceCandidateResponse::from)
                        .toList(),
                view.page(),
                view.size(),
                view.hasNext()
        );
    }
}
