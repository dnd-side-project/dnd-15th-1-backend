package kr.omong.dulpick.domain.place.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

@Schema(description = "운영자 신규 장소 등록 요청")
public record CreateAdminPlaceRequest(
        @NotBlank @Schema(example = "27190838", requiredMode = Schema.RequiredMode.REQUIRED) String kakaoPlaceId,
        @NotBlank @Schema(example = "테스트 카페", requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @NotBlank @Schema(example = "서울 강남구", requiredMode = Schema.RequiredMode.REQUIRED) String address,
        @Schema(example = "서울 강남구 테헤란로") String roadAddress,
        @Schema(example = "37.5446000") BigDecimal latitude,
        @Schema(example = "127.0557000") BigDecimal longitude,
        @Schema(example = "음식점 > 카페") String category,
        @Schema(example = "CE7") String categoryGroupCode,
        @Schema(example = "02-000-0000") String phone,
        @Schema(example = "http://place.map.kakao.com/27190838") String kakaoPlaceUrl
) {
}
