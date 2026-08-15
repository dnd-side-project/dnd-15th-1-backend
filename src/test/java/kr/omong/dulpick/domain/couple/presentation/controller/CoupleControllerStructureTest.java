package kr.omong.dulpick.domain.couple.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @Test
    void documentsConnectedPartnerAndOptionalPreviewFlow() {
        assertThat(operationDescription("preview"))
                .contains("현재 iOS 필수 연결 플로우에서는 사용하지 않지만")
                .contains("영문 대문자 5자리");
        assertThat(operationDescription("connect"))
                .contains("connected=true")
                .contains("partner")
                .contains("partner=null은 미연결 상태에서만");

        ApiResponse response = method("connect").getAnnotation(ApiResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.content()[0].examples())
                .extracting(ExampleObject::value)
                .anySatisfy(value -> assertThat(value).contains("\"partner\": {"));
    }

    @Test
    void documentsConnectedAndDisconnectedStatusExamples() {
        ApiResponse response = method("getMyStatus").getAnnotation(ApiResponse.class);
        assertThat(response).isNotNull();
        assertThat(response.content()[0].examples())
                .extracting(ExampleObject::value)
                .anySatisfy(value -> assertThat(value).contains("\"partner\": null"))
                .anySatisfy(value -> assertThat(value).contains("\"connected\": true"));
    }

    private String operationDescription(String methodName) {
        return method(methodName).getAnnotation(Operation.class).description();
    }

    private Method method(String methodName) {
        for (Method method : CoupleController.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Controller method not found: " + methodName);
    }
}
