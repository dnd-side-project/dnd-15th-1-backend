package kr.omong.dulpick.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다"),

    // 인증 및 권한
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),

    // 회원
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다"),
    MEMBER_WITHDRAWN(HttpStatus.FORBIDDEN, "탈퇴한 회원입니다"),
    MEMBER_ALREADY_WITHDRAWN(HttpStatus.BAD_REQUEST, "이미 탈퇴한 회원입니다"),
    PROFILE_REQUIRED(HttpStatus.CONFLICT, "최초 프로필 설정이 필요합니다"),
    PROFILE_ALREADY_INITIALIZED(HttpStatus.CONFLICT, "이미 프로필 설정을 완료했습니다"),

    // 커플 연결
    CONNECTION_CODE_NOT_AVAILABLE(HttpStatus.CONFLICT, "사용 가능한 연결 코드가 없습니다"),

    // 소셜 로그인
    ALREADY_LINKED_OAUTH(HttpStatus.CONFLICT, "이미 소셜 계정이 연결된 회원입니다"),
    OAUTH_VERIFICATION_FAILED(
            HttpStatus.UNAUTHORIZED,
            "소셜 로그인 토큰 검증에 실패했습니다"
    ),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다"),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 리프레시 토큰입니다");

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return name();
    }
}
