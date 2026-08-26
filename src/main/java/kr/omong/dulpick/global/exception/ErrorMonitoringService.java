package kr.omong.dulpick.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import kr.omong.dulpick.global.time.ServiceTime;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorMonitoringService {

    private static final Pattern AUTHORIZATION_PATTERN = Pattern.compile(
            "(?i)bearer\\s+[A-Za-z0-9\\-._~+/]+=*"
    );

    private final ErrorAlertSender errorAlertSender;

    public void record(
            ErrorLevel level,
            ErrorCode errorCode,
            Exception exception,
            HttpServletRequest request
    ) {
        if (level == ErrorLevel.CRITICAL && isClientDisconnect(exception)) {
            log.info("Ignoring client disconnect during response write: {}", safe(exception.getMessage()));
            return;
        }
        String method = request != null ? request.getMethod() : "N/A";
        String path = request != null ? request.getRequestURI() : "N/A";
        String clientIp = request != null ? clientIp(request) : "N/A";
        String requestId = request != null ? safe(request.getHeader("X-Request-Id")) : "N/A";
        String userAgent = request != null ? safe(trim(request.getHeader("User-Agent"), 120)) : "N/A";
        String location = stackLocation(exception);
        String detailMessage = safe(trim(exception.getMessage(), 500));

        String baseMessage = "[%s] code=%s message=%s method=%s path=%s ip=%s requestId=%s userAgent=%s location=%s"
                .formatted(
                        level,
                        errorCode.getCode(),
                        detailMessage,
                        method,
                        path,
                        clientIp,
                        requestId,
                        userAgent,
                        location
                );

        if (level == ErrorLevel.CRITICAL) {
            log.error(baseMessage, exception);
            notifyCritical(
                    baseMessage,
                    errorCode,
                    method,
                    path,
                    clientIp,
                    requestId,
                    userAgent,
                    location,
                    detailMessage
            );
            return;
        }

        if (level == ErrorLevel.WARNING) {
            log.warn(baseMessage);
            return;
        }

        log.info(baseMessage);
    }

    private boolean isClientDisconnect(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            String type = current.getClass().getSimpleName();
            String message = current.getMessage();
            if ("ClientAbortException".equals(type) || containsClientDisconnectMessage(message)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean containsClientDisconnectMessage(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("broken pipe")
                || normalized.contains("connection reset by peer")
                || normalized.contains("clientabortexception");
    }

    private void notifyCritical(
            String baseMessage,
            ErrorCode errorCode,
            String method,
            String path,
            String clientIp,
            String requestId,
            String userAgent,
            String location,
            String detailMessage
    ) {
        String alertMessage = """
                :rotating_light: **Critical Error Detected**
                **Code** `%s`
                **Message** %s
                **Time** %s
                **Request** `%s %s`
                **IP** `%s`
                **Request ID** `%s`
                **Location** `%s`
                **User Agent** `%s`
                ```text
                %s
                ```
                """
                .formatted(
                        errorCode.getCode(),
                        detailMessage,
                        OffsetDateTime.now(ServiceTime.ZONE_ID),
                        method,
                        path,
                        clientIp,
                        requestId,
                        location,
                        userAgent,
                        baseMessage
                );

        try {
            errorAlertSender.sendCriticalAlert(alertMessage);
        } catch (Exception exception) {
            log.warn("Failed to send Discord alert: {}", safe(exception.getMessage()));
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return safe(forwarded.split(",")[0].trim());
        }
        return safe(request.getRemoteAddr());
    }

    private String stackLocation(Exception exception) {
        StackTraceElement[] stackTrace = exception.getStackTrace();
        if (stackTrace == null || stackTrace.length == 0) {
            return "N/A";
        }
        StackTraceElement top = stackTrace[0];
        return "%s#%s:%d".formatted(top.getClassName(), top.getMethodName(), top.getLineNumber());
    }

    private String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        return AUTHORIZATION_PATTERN.matcher(value).replaceAll("Bearer [REDACTED]");
    }
}
