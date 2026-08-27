package kr.omong.dulpick.domain.couple.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.omong.dulpick.domain.couple.application.command.CoupleCommandService;
import kr.omong.dulpick.domain.couple.application.query.CoupleQueryService;
import kr.omong.dulpick.global.config.SwaggerTagNames;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class CoupleControllerStructureTest {

    @Test
    void dependsOnlyOnCoupleApplicationFacades() {
        assertThat(Arrays.stream(CoupleController.class.getDeclaredFields())
                .map(field -> field.getType().getName())
                .toList())
                .containsExactlyInAnyOrder(
                        CoupleCommandService.class.getName(),
                        CoupleQueryService.class.getName()
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
        assertThat(operationDescription("connect"))
                .contains("분당 10회", "일일 30회", "10분간 15회", "시간당 100회");
        assertThat(operationDescription("disconnect"))
                .contains("합산", "일일 50회", "429");
    }

    @Test
    void documentsConnectedPartnerFlow() {
        assertThat(operationDescription("connect"))
                .contains("connected=true")
                .contains("partner")
                .contains("partner=null은 미연결 상태에서만");

        ApiResponse response = response("connect", "201");
        assertThat(response).isNotNull();
        assertThat(response.content()[0].examples())
                .extracting(ExampleObject::value)
                .anySatisfy(value -> assertThat(value).contains("\"partner\": {"));
    }

    @Test
    void documentsConnectedAndDisconnectedStatusExamples() {
        ApiResponse response = response("getMyStatus", "200");
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

    private ApiResponse response(String methodName, String responseCode) {
        for (ApiResponse response : method(methodName).getAnnotationsByType(ApiResponse.class)) {
            if (response.responseCode().equals(responseCode)) {
                return response;
            }
        }
        throw new IllegalArgumentException("Swagger response not found: " + responseCode);
    }
}
