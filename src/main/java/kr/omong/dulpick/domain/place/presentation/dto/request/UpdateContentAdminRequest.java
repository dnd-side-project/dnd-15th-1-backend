package kr.omong.dulpick.domain.place.presentation.dto.request;

import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record UpdateContentAdminRequest(
        @Size(max = 4_000) @Schema(example = "서울 데이트 추천") String title,
        @Size(max = 100_000) @Schema(example = "분위기 좋은 장소를 소개합니다.") String content,
        @jakarta.validation.constraints.NotNull @Schema(example = "2026-08-24T10:00:05Z") Instant expectedUpdatedAt
) {
}
