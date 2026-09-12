package kr.omong.dulpick;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DulpickApplicationTests {

    @Autowired
    private Environment environment;

    @Test
    void runsFlywayFromTheInitialMigrationWithoutImplicitBaseline() {
        assertThat(environment.getProperty("spring.flyway.baseline-on-migrate"))
                .isEqualTo("false");
        assertThat(environment.getProperty("spring.flyway.baseline-version"))
                .isNull();
    }
}
