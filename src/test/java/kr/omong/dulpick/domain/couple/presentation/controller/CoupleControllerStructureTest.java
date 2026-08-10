package kr.omong.dulpick.domain.couple.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.omong.dulpick.domain.couple.application.command.CoupleCommandService;
import kr.omong.dulpick.domain.couple.application.query.CoupleQueryService;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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

    @Test
    void documentsConnectionRequestLimits() {
        assertThat(operationDescription("preview"))
                .contains("분당 10회", "시간당 30회", "10분간 15회", "10분간", "시간당 100회");
        assertThat(operationDescription("connect"))
                .contains("분당 10회", "일일 30회", "10분간 15회", "시간당 100회");
        assertThat(operationDescription("disconnect"))
                .contains("합산", "일일 50회", "429");
    }

    private String operationDescription(String methodName) {
        for (Method method : CoupleController.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method.getAnnotation(Operation.class).description();
            }
        }
        throw new IllegalArgumentException("Controller method not found: " + methodName);
    }
}
