package kr.omong.dulpick.domain.date.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kr.omong.dulpick.domain.date.application.command.SaveDateCourseCommand;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record SaveDateCourseRequest(
        @NotNull
        @Min(0)
        @Schema(description = "낙관적 락 버전", example = "0")
        Long version,
        @NotBlank
        @Size(max = 120)
        @Schema(description = "데이트명", example = "성수동 데이트")
        String title,
        @NotNull
        @Schema(description = "데이트 날짜(Asia/Seoul)", example = "2026-08-30")
        LocalDate date,
        @Schema(
                description = "데이트 시간(Asia/Seoul). 생략하면 날짜만 저장됩니다.",
                example = "19:30:00",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        LocalTime time,
        @NotNull
        @Schema(description = "데이트 코스 장소 ID 목록. 순서대로 저장됩니다.")
        @ArraySchema(schema = @Schema(description = "공용 장소 ID", example = "101"))
        List<Long> placeIds
) {

    public SaveDateCourseCommand toCommand() {
        return new SaveDateCourseCommand(
                version,
                title,
                date,
                time,
                placeIds
        );
    }
}
