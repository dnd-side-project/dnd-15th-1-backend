package kr.omong.dulpick.domain.testauth.security;

import kr.omong.dulpick.domain.testauth.config.TestAuthProperties;
import kr.omong.dulpick.global.exception.SecurityExceptionHandler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class TestAuthAccessKeyFilterTest {

    @Test
    void rejectsShortAccessKeyConfiguration() {
        TestAuthProperties properties = new TestAuthProperties(true, "short-key");

        assertThatThrownBy(() -> new TestAuthAccessKeyFilter(
                properties,
                mock(SecurityExceptionHandler.class)
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("short-key");
    }
}
