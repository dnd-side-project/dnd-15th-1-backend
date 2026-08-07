package kr.omong.dulpick.domain.couple.presentation.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import kr.omong.dulpick.domain.couple.application.command.CoupleCommandService;
import kr.omong.dulpick.domain.couple.application.query.CoupleQueryService;
import kr.omong.dulpick.global.config.SwaggerTagNames;
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

    @Test
    void groupsConnectionCodeAndCoupleEndpointsUnderSameSwaggerTag() {
        Tag coupleTag = CoupleController.class.getAnnotation(Tag.class);
        Tag connectionCodeTag = ConnectionCodeController.class.getAnnotation(Tag.class);

        assertThat(coupleTag.name()).isEqualTo(SwaggerTagNames.COUPLE_CONNECTION);
        assertThat(connectionCodeTag.name()).isEqualTo(SwaggerTagNames.COUPLE_CONNECTION);
    }
}
