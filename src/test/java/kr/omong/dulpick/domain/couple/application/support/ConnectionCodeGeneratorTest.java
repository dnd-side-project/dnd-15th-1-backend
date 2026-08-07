package kr.omong.dulpick.domain.couple.application.support;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionCodeGeneratorTest {

    private final ConnectionCodeGenerator generator =
            new ConnectionCodeGenerator(new SecureRandom());

    @Test
    void generatesFiveUppercaseLetters() {
        for (int count = 0; count < 100; count++) {
            assertThat(generator.generate()).matches("^[A-Z]{5}$");
        }
    }
}
