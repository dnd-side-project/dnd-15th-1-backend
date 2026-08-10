package kr.omong.dulpick.domain.place.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.omong.dulpick.domain.place.application.PublicContentQueryService;
import kr.omong.dulpick.domain.place.presentation.dto.response.PublicContentPageResponse;
import kr.omong.dulpick.domain.place.presentation.dto.response.PublicContentResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = SwaggerTagNames.PLACE, description = "공용 게시물 큐레이션 조회 API")
@SecurityRequirement(name = "bearerAuth")
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
            summary = "공개 게시물 단건 조회",
            description = "공개 상태인 원본 게시물과 연결 장소를 게시물 ID로 조회합니다."
    )
    @GetMapping("/{contentId}")
    public ResponseEntity<PublicContentResponse> findPublicContent(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "모든 사용자가 공유하는 공개 게시물 ID")
            @PathVariable Long contentId
    ) {
        return ResponseEntity.ok(PublicContentResponse.from(
                queryService.findPublicContent(memberId(jwt), contentId)
        ));
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
