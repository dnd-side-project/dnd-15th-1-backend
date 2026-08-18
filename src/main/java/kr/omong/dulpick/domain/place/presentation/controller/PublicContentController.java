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
import jakarta.validation.constraints.Size;
import kr.omong.dulpick.domain.place.application.PublicContentQueryService;
import kr.omong.dulpick.domain.place.presentation.dto.response.PublicContentPageResponse;
import kr.omong.dulpick.domain.place.presentation.dto.response.PublicContentResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import kr.omong.dulpick.global.exception.ErrorResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    public PublicContentController(PublicContentQueryService queryService) {
        this.queryService = queryService;
    }

    @Operation(
            summary = "공개 게시물 큐레이션 조회",
            description = "모든 회원이 제출한 게시물 중 공개 상태인 원본 게시물과 연결 장소를 조회합니다."
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
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(PublicContentPageResponse.from(
                queryService.findPublicContents(memberId(jwt), pageable)
        ));
    }

    @Operation(
            summary = "공개 게시물 키워드 검색",
            description = "공개 상태인 게시물의 제목과 본문에서 키워드를 검색하고 최신순으로 반환합니다."
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
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(PublicContentPageResponse.from(
                queryService.searchPublicContents(memberId(jwt), query, pageable)
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
                queryService.findPublicContent(memberId(jwt), contentId)
        ));
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
