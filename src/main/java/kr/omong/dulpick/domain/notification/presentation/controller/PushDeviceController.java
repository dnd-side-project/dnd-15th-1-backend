package kr.omong.dulpick.domain.notification.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.omong.dulpick.domain.notification.application.command.PushDeviceService;
import kr.omong.dulpick.domain.notification.application.command.PushDeviceView;
import kr.omong.dulpick.domain.notification.presentation.dto.request.PushDeviceRequest;
import kr.omong.dulpick.domain.notification.presentation.dto.response.PushDeviceResponse;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import kr.omong.dulpick.global.exception.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(
        name = SwaggerTagNames.NOTIFICATION,
        description = "알림함 조회·읽음 처리, 알림 수신 설정, iOS 푸시 디바이스 관리 API"
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/push-devices")
public class PushDeviceController {

    private final PushDeviceService pushDeviceService;

    public PushDeviceController(PushDeviceService pushDeviceService) {
        this.pushDeviceService = pushDeviceService;
    }

    @Operation(
            summary = "현재 iOS 디바이스의 푸시 정보 등록·갱신",
            description = "로그인한 회원에게 현재 앱 설치의 FCM 토큰을 등록합니다. "
                    + "앱 설치별로 생성한 동일한 deviceId를 계속 사용하며, FCM 토큰이 변경되면 같은 API로 갱신합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "푸시 디바이스 등록 성공",
                    content = @Content(schema = @Schema(implementation = PushDeviceResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "플랫폼·공급자·등록 토큰이 누락되었거나 허용되지 않은 값입니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "푸시 디바이스 등록 상태가 충돌했습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "푸시 디바이스를 등록할 수 없습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{deviceId}")
    public ResponseEntity<PushDeviceResponse> register(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(
                    description = "앱 설치 시 생성하고 유지하는 디바이스 UUID",
                    required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
            )
            @PathVariable @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID deviceId,
            @Valid @RequestBody PushDeviceRequest request
    ) {
        PushDeviceView device = pushDeviceService.register(
                memberId(jwt),
                request.toCommand(deviceId)
        );
        return ResponseEntity.ok(PushDeviceResponse.from(device));
    }

    @Operation(
            summary = "현재 iOS 디바이스의 푸시 등록 해제",
            description = "로그아웃하는 현재 앱 설치를 푸시 발송 대상에서 제외합니다. "
                    + "이미 해제된 내 디바이스를 다시 요청해도 성공하며, 응답 본문 없이 204를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "푸시 디바이스 해제 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token이 없거나 유효하지 않습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "푸시 디바이스를 찾을 수 없습니다",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> unregister(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(
                    description = "푸시 등록을 해제할 디바이스 UUID",
                    required = true,
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
            )
            @PathVariable @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID deviceId
    ) {
        pushDeviceService.unregister(memberId(jwt), deviceId);
        return ResponseEntity.noContent().build();
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
