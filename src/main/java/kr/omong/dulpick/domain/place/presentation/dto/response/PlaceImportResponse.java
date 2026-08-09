package kr.omong.dulpick.domain.place.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.application.PlaceCandidateView;
import kr.omong.dulpick.domain.place.application.PlaceImportView;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;

import java.util.List;

public record PlaceImportResponse(
        @Schema(description = "분석 작업 ID")
        Long importId,
        @Schema(description = "추적 파라미터를 제거한 원본 게시물 링크")
        String originalUrl,
        @Schema(description = "콘텐츠 유형")
        ContentSourceType sourceType,
        @Schema(description = "게시물 제목")
        String title,
        @Schema(description = "게시물 설명 또는 캡션")
        String content,
        @Schema(description = "게시물 썸네일 URL", nullable = true)
        String thumbnailUrl,
        @Schema(description = "분석 상태")
        PlaceImportStatus status,
        @Schema(description = "실패 코드", nullable = true)
        String failureCode,
        List<PlaceCandidateResponse> candidates
) {

    public static PlaceImportResponse from(PlaceImportView view) {
        return new PlaceImportResponse(
                view.importId(),
                view.originalUrl(),
                view.sourceType(),
                view.title(),
                view.content(),
                view.thumbnailUrl(),
                view.status(),
                view.failureCode(),
                view.candidates().stream().map(PlaceCandidateResponse::from).toList()
        );
    }

    public record PlaceCandidateResponse(
            Long candidateId,
            Long placeId,
            String name,
            String address,
            String roadAddress,
            String kakaoPlaceId,
            String category
    ) {

        private static PlaceCandidateResponse from(PlaceCandidateView view) {
            return new PlaceCandidateResponse(
                    view.candidateId(),
                    view.placeId(),
                    view.name(),
                    view.address(),
                    view.roadAddress(),
                    view.kakaoPlaceId(),
                    view.category()
            );
        }
    }
}
