package kr.omong.dulpick.domain.place.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.omong.dulpick.domain.place.application.RegionTagQueryService;
import kr.omong.dulpick.domain.place.presentation.dto.response.RegionTagResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import kr.omong.dulpick.global.exception.ErrorResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = SwaggerTagNames.PLACE, description = "지역 태그 조회 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/region-tags")
public class RegionTagController {

    private final RegionTagQueryService regionTagQueryService;

    public RegionTagController(RegionTagQueryService regionTagQueryService) {
        this.regionTagQueryService = regionTagQueryService;
    }

    @Operation(
            summary = "지역 태그 목록 조회",
            description = "활성 지역 태그를 운영 표시 순서대로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "지역 태그 목록 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = RegionTagResponse.class))
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<List<RegionTagResponse>> getAll() {
        return ResponseEntity.ok(regionTagQueryService.getAll()
                .stream()
                .map(RegionTagResponse::from)
                .toList());
    }

    @Operation(
            summary = "지역 태그 상세 조회",
            description = "활성 지역 태그의 정보와 현재 연결 장소 수를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "지역 태그 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = RegionTagResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "활성 지역 태그를 찾을 수 없습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{regionTagId}")
    public ResponseEntity<RegionTagResponse> get(
            @Parameter(description = "지역 태그 ID", required = true, example = "1")
            @PathVariable @Schema(example = "1") Long regionTagId
    ) {
        return ResponseEntity.ok(RegionTagResponse.from(regionTagQueryService.get(regionTagId)));
    }
}
