package kr.omong.dulpick.global.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private ErrorMonitoringService errorMonitoringService;

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler(errorMonitoringService);
    }

    @Test
    void recordsWarningForBusiness4xxException() {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/members");
        BusinessException exception = new BusinessException(ErrorCode.MEMBER_NOT_FOUND);

        globalExceptionHandler.handleBusiness(exception, request);

        verify(errorMonitoringService).record(
                ErrorLevel.WARNING,
                ErrorCode.MEMBER_NOT_FOUND,
                exception,
                request
        );
    }

    @Test
    void recordsInfoForNotFoundException() {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/missing");
        NoResourceFoundException exception = mock(NoResourceFoundException.class);

        globalExceptionHandler.handleNotFound(exception, request);

        verify(errorMonitoringService).record(
                ErrorLevel.INFO,
                ErrorCode.NOT_FOUND,
                exception,
                request
        );
    }

    @Test
    void recordsCriticalForUnhandledException() {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/boom");
        RuntimeException exception = new RuntimeException("boom");

        globalExceptionHandler.handleUnexpected(exception, request);

        verify(errorMonitoringService).record(
                ErrorLevel.CRITICAL,
                ErrorCode.INTERNAL_ERROR,
                exception,
                request
        );
    }
}
