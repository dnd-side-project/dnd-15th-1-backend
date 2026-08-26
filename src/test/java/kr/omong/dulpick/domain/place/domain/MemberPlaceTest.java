package kr.omong.dulpick.domain.place.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MemberPlaceTest {

    @Test
    void storesBlankAliasAsNull() {
        MemberPlace memberPlace = MemberPlace.save(
                1L,
                mock(Place.class),
                null,
                "  ",
                Instant.parse("2026-08-21T00:00:00Z")
        );

        assertThat(memberPlace.getAlias()).isNull();
    }

    @Test
    void trimsAliasBeforeSaving() {
        MemberPlace memberPlace = MemberPlace.save(
                1L,
                mock(Place.class),
                null,
                "  데이트 장소  ",
                Instant.parse("2026-08-21T00:00:00Z")
        );

        assertThat(memberPlace.getAlias()).isEqualTo("데이트 장소");
    }
}
