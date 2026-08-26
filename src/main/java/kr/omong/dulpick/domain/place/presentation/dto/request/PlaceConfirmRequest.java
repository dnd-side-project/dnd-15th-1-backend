package kr.omong.dulpick.domain.place.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlaceConfirmRequest(
        @NotEmpty
        @Size(max = 20)
        @Schema(
                description = "필수 입력. 저장할 검증 완료 장소 후보 목록입니다. 최소 1개, 최대 20개를 선택할 수 있습니다.",
                example = "[{\"candidateId\":101,\"alias\":\"주말 데이트 카페\"}]",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        List<@Valid Selection> selections
) {

    public record Selection(
            @NotNull
            @Schema(
                    description = "필수 입력. URL 경로의 importId에 속한 검증 완료 후보 ID입니다.",
                    requiredMode = Schema.RequiredMode.REQUIRED,
                    example = "101"
            )
            Long candidateId,
            @Size(max = 100)
            @Schema(
                    description = "선택 입력. 현재 회원과 연결된 상대방에게 보일 장소 별칭입니다. 생략하거나 null이면 별칭 없이 저장합니다.",
                    nullable = true,
                    requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
            String alias
    ) {
    }
}
