package kr.omong.dulpick.domain.place.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import kr.omong.dulpick.domain.place.application.PublicContentQueryService;
import kr.omong.dulpick.domain.place.config.ContentThumbnailProperties;
import kr.omong.dulpick.domain.place.domain.ContentRecommendationSort;
import kr.omong.dulpick.domain.place.presentation.dto.response.PublicContentPageResponse;
import kr.omong.dulpick.domain.place.presentation.dto.response.PublicContentResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import kr.omong.dulpick.global.exception.ErrorResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = SwaggerTagNames.PLACE, description = "공용 게시물 큐레이션 조회 API")
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/api/v1/contents")
public class PublicContentController {

    private final PublicContentQueryService queryService;
    private final ContentThumbnailProperties thumbnailProperties;

    public PublicContentController(
            PublicContentQueryService queryService,
            ContentThumbnailProperties thumbnailProperties
    ) {
        this.queryService = queryService;
        this.thumbnailProperties = thumbnailProperties;
    }

    @Operation(
            summary = "공개 게시물 큐레이션 조회",
            description = "공개 게시물과 연결 장소를 조회합니다. "
                    + "기본 정렬은 연결된 장소 저장 수 합계 내림차순(인기순)입니다. "
                    + "sort=PREFERENCE이면 현재 회원의 데이트 성향 일치도가 높은 순으로 두고, "
                    + "같은 성향이면 저장 수 내림차순입니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "공개 게시물 목록 조회 성공. 결과가 없으면 빈 배열을 반환합니다.",
                    content = @Content(schema = @Schema(implementation = PublicContentPageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<PublicContentPageResponse> findPublicContents(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "추천 정렬. POPULAR는 저장 많은 순, PREFERENCE는 성향 일치 후 저장 많은 순입니다.", example = "POPULAR")
            @RequestParam(defaultValue = "POPULAR")
            @Schema(allowableValues = {"POPULAR", "PREFERENCE"}, example = "POPULAR")
            ContentRecommendationSort sort,
            @Parameter(description = "0부터 시작하는 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) @Schema(example = "0") int page,
            @Parameter(description = "페이지당 게시물 수. 최대 50입니다.", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) @Schema(example = "20") int size
    ) {
        return ResponseEntity.ok(PublicContentPageResponse.from(
                queryService.findPublicContents(memberId(jwt), PageRequest.of(page, size), sort),
                thumbnailProperties.baseUrl()
        ));
    }

    @Operation(
            summary = "공개 게시물 키워드 검색",
            description = "공개 상태인 게시물의 제목과 본문에서 키워드를 검색하고 최신순으로 반환합니다. "
                    + "MySQL ngram FULLTEXT 인덱스를 사용합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "공개 게시물 검색 성공. 결과가 없으면 빈 배열을 반환합니다.",
                    content = @Content(schema = @Schema(implementation = PublicContentPageResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "검색어가 비어 있거나 200자를 초과했습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/search")
    public ResponseEntity<PublicContentPageResponse> searchPublicContents(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "제목과 본문에서 찾을 검색어", required = true, example = "서울 데이트")
            @RequestParam @NotBlank @Size(max = 200) @Schema(example = "서울 데이트") String query,
            @Parameter(description = "0부터 시작하는 페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) @Schema(example = "0") int page,
            @Parameter(description = "페이지당 게시물 수. 최대 50입니다.", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) @Schema(example = "20") int size
    ) {
        return ResponseEntity.ok(PublicContentPageResponse.from(
                queryService.searchPublicContents(memberId(jwt), query, PageRequest.of(page, size)),
                thumbnailProperties.baseUrl()
        ));
    }

    @Operation(
            summary = "공개 게시물 단건 조회",
            description = "공개 상태인 원본 게시물과 연결 장소를 게시물 ID로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "공개 게시물 단건 조회 성공",
                    content = @Content(schema = @Schema(implementation = PublicContentResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "공개 게시물을 찾을 수 없습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{contentId}")
    public ResponseEntity<PublicContentResponse> findPublicContent(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "모든 사용자가 공유하는 공개 게시물 ID", required = true, example = "2001")
            @PathVariable @Schema(example = "2001") Long contentId
    ) {
        return ResponseEntity.ok(PublicContentResponse.from(
                queryService.findPublicContent(memberId(jwt), contentId),
                thumbnailProperties.baseUrl()
        ));
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
