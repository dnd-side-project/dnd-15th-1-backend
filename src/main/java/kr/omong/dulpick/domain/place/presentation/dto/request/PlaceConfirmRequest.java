package kr.omong.dulpick.domain.place.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlaceConfirmRequest(
        @NotEmpty
        @Size(max = 10)
        @Schema(description = "저장할 검증 완료 장소 후보 목록")
        List<@Valid Selection> selections
) {

    public record Selection(
            @NotNull
            @Schema(description = "URL 경로의 importId에 속한 장소 후보 ID")
            Long candidateId,
            @Size(max = 100)
            @Schema(description = "현재 회원과 연결된 상대방에게 보일 장소 별칭")
            String alias,
            @Size(max = 1_000)
            @Schema(description = "장소 메모")
            String memo
    ) {
    }
}
