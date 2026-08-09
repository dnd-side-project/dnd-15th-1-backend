package kr.omong.dulpick.domain.couple.application.support;

import kr.omong.dulpick.domain.couple.application.exception.InvalidConnectionCodeException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConnectionCodeNormalizerTest {

    private final ConnectionCodeNormalizer normalizer = new ConnectionCodeNormalizer();

    @Test
    void normalizesFiveLettersToUppercase() {
        assertThat(normalizer.normalize(" abcde ")).isEqualTo("ABCDE");
    }

    @Test
    void rejectsLegacySixLetterCode() {
        assertThatThrownBy(() -> normalizer.normalize("ABCDEF"))
                .isInstanceOf(InvalidConnectionCodeException.class);
    }
}
