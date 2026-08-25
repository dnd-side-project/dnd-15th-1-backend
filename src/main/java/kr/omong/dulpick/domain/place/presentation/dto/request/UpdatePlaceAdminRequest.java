package kr.omong.dulpick.domain.place.presentation.dto.request;

import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record UpdatePlaceAdminRequest(
        @Size(max = 255) @Schema(example = "서울숲 카페") String name,
        @Size(max = 500) @Schema(example = "서울특별시 성동구 성수동") String address,
        @Size(max = 500) @Schema(example = "서울특별시 성동구 서울숲2길 10") String roadAddress,
        @Size(max = 100) @Schema(example = "음식점 > 카페") String category,
        @Size(max = 3) @Schema(example = "CE7") String categoryGroupCode,
        @Size(max = 50) @Schema(example = "02-1234-5678") String phone,
        @Size(max = 1_000) @Schema(example = "https://place.map.kakao.com/1234567890") String kakaoPlaceUrl,
        @jakarta.validation.constraints.NotNull @Schema(example = "2026-08-24T10:00:05Z") Instant expectedUpdatedAt
) {
}
