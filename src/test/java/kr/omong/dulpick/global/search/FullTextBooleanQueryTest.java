package kr.omong.dulpick.global.search;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FullTextBooleanQueryTest {

    @Test
    void convertsTokensIntoRequiredBooleanTerms() {
        assertThat(FullTextBooleanQuery.from("  서울   데이트  ")).isEqualTo("+서울 +데이트");
        assertThat(FullTextBooleanQuery.from("카페")).isEqualTo("+카페");
    }

    @Test
    void stripsBooleanOperatorsAndIgnoresSingleCharacterTokens() {
        assertThat(FullTextBooleanQuery.from("+성수-카페")).isEqualTo("+성수 +카페");
        assertThat(FullTextBooleanQuery.from("a 성수")).isEqualTo("+성수");
        assertThat(FullTextBooleanQuery.from("a")).isEqualTo("+__no_match__");
        assertThat(FullTextBooleanQuery.from("   ")).isEqualTo("+__no_match__");
    }
}
