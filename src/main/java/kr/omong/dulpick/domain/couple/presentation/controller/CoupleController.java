package kr.omong.dulpick.domain.couple.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.couple.application.command.ConnectCoupleCommand;
import kr.omong.dulpick.domain.couple.application.command.CoupleCommandService;
import kr.omong.dulpick.domain.couple.application.query.CoupleQueryService;
import kr.omong.dulpick.domain.couple.application.query.view.ConnectionCodePreview;
import kr.omong.dulpick.domain.couple.application.query.view.CoupleConnectionStatus;
import kr.omong.dulpick.domain.couple.presentation.dto.ConnectionCodePreviewResponse;
import kr.omong.dulpick.domain.couple.presentation.dto.ConnectionCodeRequest;
import kr.omong.dulpick.domain.couple.presentation.dto.CoupleConnectionStatusResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import kr.omong.dulpick.global.exception.ErrorResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = SwaggerTagNames.COUPLE_CONNECTION,
        description = "연결 코드 확인, 커플 연결 확정 및 현재 연결 상태 조회 API"
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1")
public class CoupleController {

    private final CoupleCommandService coupleCommandService;
    private final CoupleQueryService coupleQueryService;

    public CoupleController(
            CoupleCommandService coupleCommandService,
            CoupleQueryService coupleQueryService
    ) {
        this.coupleCommandService = coupleCommandService;
        this.coupleQueryService = coupleQueryService;
    }

    @Operation(
            summary = "연결 코드 상대방 미리보기",
            description = """
                    연결을 확정하기 전에 연결 코드 소유자의 닉네임과 프로필 아이콘을 조회합니다.
                    현재 iOS 필수 연결 플로우에서는 사용하지 않지만 호환성을 위해 유지하는 선택 API입니다.
                    요청 성공만으로 커플 연결이 완료되지는 않으며, 연결 확정은 POST /api/v1/couples로 별도 요청해야 합니다.
                    connectionCode는 필수이며 영문 대문자 5자리입니다. 입력 시 앞뒤 공백을 제거하고 대문자로 정규화합니다.
                    회원별 요청 제한은 분당 10회, 시간당 30회입니다.
                    잘못된 코드가 회원별 10분간 15회 누적되면 10분간 차단됩니다.
                    같은 IP에서 잘못된 코드가 시간당 100회 누적돼도 429를 반환합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "연결 코드 소유자 미리보기 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConnectionCodePreviewResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "연결 코드가 비어 있거나 영문 대문자 5자리 형식이 아닙니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "연결 코드에 해당하는 회원을 찾을 수 없습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "연결 코드 조회 요청 횟수 제한을 초과했습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/couple-connections/preview")
    public ResponseEntity<ConnectionCodePreviewResponse> preview(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConnectionCodeRequest request,
            HttpServletRequest httpRequest
    ) {
        ConnectionCodePreview preview = coupleQueryService.preview(
                memberId(jwt),
                request.connectionCode(),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(ConnectionCodePreviewResponse.from(preview));
    }

    @Operation(
            summary = "커플 연결 확정",
            description = """
                    상대방의 활성 연결 코드로 커플 관계를 생성합니다.
                    connectionCode는 필수이며 영문 대문자 5자리입니다.
                    연결에 성공하면 connected=true이고 partner에는 연결된 상대방의 최신 nickname과 profileIcon이 표시됩니다.
                    partner=null은 미연결 상태에서만 사용합니다.
                    회원별 요청 제한은 분당 10회, 일일 30회입니다.
                    연결과 연결 해제는 합산하여 회원별 일일 50회까지 가능합니다.
                    잘못된 코드가 회원별 10분간 15회 누적되면 10분간 차단됩니다.
                    같은 IP에서 잘못된 코드가 시간당 100회 누적돼도 429를 반환합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "커플 연결 성공. partner에는 연결된 상대방의 최신 프로필이 포함됩니다.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CoupleConnectionStatusResponse.class),
                            examples = @ExampleObject(
                                    name = "연결 성공",
                                    value = """
                                            {
                                              "connected": true,
                                              "me": {
                                                "nickname": "둘픽이",
                                                "profileIcon": 1
                                              },
                                              "partner": {
                                                "nickname": "오몽이",
                                                "profileIcon": 3
                                              },
                                              "connectedAt": "2026-08-16T14:30:00",
                                              "daysTogether": 1
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "연결 코드가 비어 있거나 영문 대문자 5자리 형식이 아닙니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "연결 코드에 해당하는 회원을 찾을 수 없습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "자기 자신과 연결하거나 이미 연결된 회원을 다시 연결할 수 없습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "커플 연결 요청 횟수 제한을 초과했습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/couples")
    public ResponseEntity<CoupleConnectionStatusResponse> connect(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConnectionCodeRequest request,
            HttpServletRequest httpRequest
    ) {
        CoupleConnectionStatus status = coupleCommandService.connect(
                memberId(jwt),
                new ConnectCoupleCommand(request.connectionCode()),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.status(201).body(CoupleConnectionStatusResponse.from(status));
    }

    @Operation(
            summary = "내 커플 연결 상태 조회",
            description = """
                    연결 여부와 나·상대방의 최신 프로필을 조회합니다.
                    연결 상태이면 connected=true와 상대방 partner 객체를 반환합니다.
                    미연결이면 connected=false이고 partner, connectedAt, daysTogether는 null입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "현재 커플 연결 상태 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CoupleConnectionStatusResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "연결 상태",
                                            value = """
                                                    {
                                                      "connected": true,
                                                      "me": {"nickname": "둘픽이", "profileIcon": 1},
                                                      "partner": {"nickname": "오몽이", "profileIcon": 3},
                                                      "connectedAt": "2026-08-16T14:30:00",
                                                      "daysTogether": 1
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "미연결 상태",
                                            value = """
                                                    {
                                                      "connected": false,
                                                      "me": {"nickname": "둘픽이", "profileIcon": 1},
                                                      "partner": null,
                                                      "connectedAt": null,
                                                      "daysTogether": null
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "최초 프로필 설정이 완료되지 않았습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/couples/me")
    public ResponseEntity<CoupleConnectionStatusResponse> getMyStatus(
            @AuthenticationPrincipal Jwt jwt
    ) {
        CoupleConnectionStatus status = coupleQueryService.getMyStatus(memberId(jwt));
        return ResponseEntity.ok(CoupleConnectionStatusResponse.from(status));
    }

    @Operation(
            summary = "커플 연결 해제",
            description = """
                    현재 활성 커플 연결을 해제합니다.
                    활성 상태인 두 회원은 각각 새로운 연결 코드를 발급받을 수 있습니다.
                    연결과 연결 해제는 합산하여 회원별 일일 50회까지 가능하며,
                    제한을 초과하면 429를 반환합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "커플 연결 해제 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "현재 연결된 커플이 없습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "커플 연결 해제 요청 횟수 제한을 초과했습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/couples/me")
    public ResponseEntity<Void> disconnect(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest
    ) {
        coupleCommandService.disconnect(memberId(jwt), httpRequest.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
