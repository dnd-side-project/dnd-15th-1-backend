package kr.omong.dulpick.domain.couple.presentation.controller;

import kr.omong.dulpick.domain.couple.application.command.CoupleCommandService;
import kr.omong.dulpick.domain.couple.application.query.CoupleQueryService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class CoupleControllerStructureTest {

    @Test
    void dependsOnlyOnCoupleApplicationFacades() {
        assertThat(CoupleController.class.getDeclaredFields())
                .extracting(Field::getType)
                .containsExactlyInAnyOrder(
                        CoupleCommandService.class,
                        CoupleQueryService.class
                );
    }
}
