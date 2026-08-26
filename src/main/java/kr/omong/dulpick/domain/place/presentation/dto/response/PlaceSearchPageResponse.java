package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.application.PlaceSearchPage;

import java.util.List;

public record PlaceSearchPageResponse(
        @ArraySchema(schema = @Schema(implementation = PlaceSearchResponse.class))
        @Schema(description = "현재 페이지의 장소 검색 결과. 없으면 빈 배열입니다.", example = "[]", requiredMode = Schema.RequiredMode.REQUIRED)
        List<PlaceSearchResponse> places,
        @Schema(description = "0부터 시작하는 현재 페이지 번호", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        int page,
        @Schema(description = "페이지당 장소 수", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        int size,
        @Schema(description = "다음 10건을 더 요청할 수 있으면 true입니다.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasNext
) {

    public static PlaceSearchPageResponse from(PlaceSearchPage page) {
        return new PlaceSearchPageResponse(
                page.places().stream().map(PlaceSearchResponse::from).toList(),
                page.page(),
                page.size(),
                page.hasNext()
        );
    }
}
