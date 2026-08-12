package kr.omong.dulpick.global.exception;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

class ErrorMonitoringServiceTest {

    @Test
    void sendsSlackAlertOnlyForCriticalLevel() {
        CapturingAlertSender alertSender = new CapturingAlertSender();
        ErrorMonitoringService service = new ErrorMonitoringService(alertSender);
        MockHttpServletRequest request = request();
        RuntimeException exception = new RuntimeException("critical failure");

        service.record(ErrorLevel.CRITICAL, ErrorCode.INTERNAL_ERROR, exception, request);
        service.record(ErrorLevel.WARNING, ErrorCode.ACCESS_DENIED, exception, request);
        service.record(ErrorLevel.INFO, ErrorCode.NOT_FOUND, exception, request);

        assertThat(alertSender.callCount).isEqualTo(1);
        assertThat(alertSender.lastMessage).contains("Critical Error Detected");
    }

    @Test
    void swallowsSlackAlertFailure() {
        ErrorAlertSender failingSender = message -> {
            throw new IllegalStateException("slack down");
        };
        ErrorMonitoringService service = new ErrorMonitoringService(failingSender);
        MockHttpServletRequest request = request();

        assertThatCode(() -> service.record(
                ErrorLevel.CRITICAL,
                ErrorCode.INTERNAL_ERROR,
                new RuntimeException("boom"),
                request
        )).doesNotThrowAnyException();
    }

    @Test
    void masksBearerTokenInAlertMessage() {
        CapturingAlertSender alertSender = new CapturingAlertSender();
        ErrorMonitoringService service = new ErrorMonitoringService(alertSender);
        MockHttpServletRequest request = request();
        RuntimeException exception = new RuntimeException(
                "authorization=Bearer abcdefghijklmnopqrstuvwxyz token=secret123"
        );

        service.record(ErrorLevel.CRITICAL, ErrorCode.INTERNAL_ERROR, exception, request);

        assertThat(alertSender.lastMessage).doesNotContain("abcdefghijklmnopqrstuvwxyz");
        assertThat(alertSender.lastMessage).contains("[REDACTED]");
        assertThat(alertSender.lastMessage).contains("token=secret123");
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Request-Id", "req-123");
        request.addHeader("User-Agent", "Mozilla/5.0");
        return request;
    }

    private static class CapturingAlertSender implements ErrorAlertSender {
        private int callCount;
        private String lastMessage = "";

        @Override
        public void sendCriticalAlert(String message) {
            callCount++;
            lastMessage = message;
        }
    }
}
