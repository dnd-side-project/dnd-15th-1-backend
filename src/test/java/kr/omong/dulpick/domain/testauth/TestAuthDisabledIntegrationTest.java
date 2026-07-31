package kr.omong.dulpick.domain.testauth;

import kr.omong.dulpick.domain.testauth.application.TestAuthService;
import kr.omong.dulpick.domain.testauth.presentation.TestAuthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "features.test-auth.enabled=false")
class TestAuthDisabledIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void doesNotRegisterTestAuthenticationComponentsWhenDisabled() {
        assertThat(applicationContext.getBeansOfType(TestAuthController.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(TestAuthService.class)).isEmpty();
    }
}
