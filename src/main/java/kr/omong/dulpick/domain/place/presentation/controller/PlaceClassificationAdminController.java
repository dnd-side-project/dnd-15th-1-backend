package kr.omong.dulpick.domain.place.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.place.application.PlaceClassificationAdminService;
import kr.omong.dulpick.domain.place.domain.PlaceClassificationStatus;
import kr.omong.dulpick.domain.place.presentation.dto.request.UpdatePlaceClassificationRequest;
import kr.omong.dulpick.domain.place.presentation.dto.response.PlaceClassificationAdminPageResponse;
import kr.omong.dulpick.domain.place.presentation.dto.response.PlaceClassificationAdminResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import kr.omong.dulpick.global.exception.ErrorResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = SwaggerTagNames.OPS, description = "운영자 장소 데이트 유형 분류 API")
@SecurityRequirement(name = "basicAuth")
@Validated
@RestController
@RequestMapping("/api/v1/admin/place-classifications")
public class PlaceClassificationAdminController {

    private final PlaceClassificationAdminService adminService;

    public PlaceClassificationAdminController(PlaceClassificationAdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(
            summary = "장소 데이트 유형 목록",
            description = "사용자가 저장한 콘텐츠에 연결된 장소만 조회합니다. "
                    + "운영자가 네 축(실내/실외, 액티비티/정적, 낮/밤, 식사/볼거리)을 수동으로 답니다. "
                    + "HTTP Basic 인증이 필요합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "목록 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PlaceClassificationAdminPageResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "운영자 아이디 또는 비밀번호가 올바르지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<PlaceClassificationAdminPageResponse> list(
            @Parameter(description = "분류 상태 필터. 생략하면 전체", example = "UNCLASSIFIED")
            @RequestParam(required = false) @Schema(example = "UNCLASSIFIED") PlaceClassificationStatus status,
            @Parameter(description = "장소명·주소 검색어", example = "성수")
            @RequestParam(required = false) @Schema(example = "성수") String query,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(PlaceClassificationAdminPageResponse.from(
                adminService.list(status, query, pageable)
        ));
    }

    @Operation(summary = "장소 데이트 유형 단건 조회")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "단건 조회 성공",
                    content = @Content(schema = @Schema(implementation = PlaceClassificationAdminResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "장소를 찾을 수 없습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{placeId:[0-9]+}")
    public ResponseEntity<PlaceClassificationAdminResponse> get(
            @Parameter(description = "공용 장소 ID", required = true, example = "101")
            @PathVariable @Schema(example = "101") Long placeId
    ) {
        return ResponseEntity.ok(PlaceClassificationAdminResponse.from(adminService.get(placeId)));
    }

    @Operation(
            summary = "장소 데이트 유형 수동 수정",
            description = "변경할 축만 보냅니다. 값은 출처 MANUAL로 저장되고, null이면 해당 축을 비웁니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = PlaceClassificationAdminResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "변경할 축이 없거나 값이 올바르지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PatchMapping("/{placeId:[0-9]+}")
    public ResponseEntity<PlaceClassificationAdminResponse> update(
            @Parameter(description = "공용 장소 ID", required = true, example = "101")
            @PathVariable @Schema(example = "101") Long placeId,
            @Valid @RequestBody UpdatePlaceClassificationRequest request
    ) {
        return ResponseEntity.ok(PlaceClassificationAdminResponse.from(
                adminService.update(placeId, request)
        ));
    }
}
