package kr.omong.dulpick.domain.place.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.omong.dulpick.domain.place.application.PublicContentQueryService;
import kr.omong.dulpick.domain.place.application.PublicContentView;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public ResponseEntity<List<PublicContentView>> findPublicContents() {
        return ResponseEntity.ok(queryService.findPublicContents());
    }
}
